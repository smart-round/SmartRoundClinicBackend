package ke.co.smartroundclinic.infra.apns

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date
import ke.co.smartroundclinic.infra.EnvLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

/** Which app the recipient device token belongs to — each has its own bundle id (apns-topic), but shares one Team-wide auth key. */
enum class ApnsAppTarget { PATIENT, DOCTOR }

/**
 * Distinguishes a dead token (APNs rejected it with "BadDeviceToken" or "Unregistered" — per
 * Apple's guidance this token is permanently invalid and the caller should stop storing/using it)
 * from every other failure (config, network, transient APNs error), which should just be retried
 * next call rather than deleting a token that might still be good.
 */
sealed class ApnsSendResult {
    data object Success : ApnsSendResult()
    data object InvalidToken : ApnsSendResult()
    data class Error(val message: String) : ApnsSendResult()
}

private data class ApnsVoipCredentials(
    val teamId: String,
    val keyId: String,
    val authKeyP8: String,
    val useSandbox: Boolean,
)

/**
 * Sends `apns-push-type: voip` pushes directly to APNs (bypassing FCM, which has no VoIP-token
 * concept) so iOS can wake the app via PushKit/CallKit even when backgrounded or killed. Requires
 * a VoIP Services Certificate's associated .p8 auth key from the Apple Developer portal — see
 * APNS_TEAM_ID / APNS_KEY_ID / APNS_AUTH_KEY_P8 env vars. One key authenticates pushes to both the
 * doctor and patient apps (APNs auth-key tokens are Team-wide); only the per-app apns-topic
 * differs, resolved from APNS_BUNDLE_ID_PATIENT / APNS_BUNDLE_ID_DOCTOR by the caller's
 * [ApnsAppTarget]. Entirely optional: if unset, [send] returns an [ApnsSendResult.Error] and callers fall
 * back to the FCM silent-push channel.
 */
class ApnsVoipClient {
    private val log = LoggerFactory.getLogger(ApnsVoipClient::class.java)
    private val http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()
    private val json = Json { encodeDefaults = true }

    private val credentials: ApnsVoipCredentials? by lazy {
        val teamId = EnvLoader.get("APNS_TEAM_ID")
        val keyId = EnvLoader.get("APNS_KEY_ID")
        val authKey = EnvLoader.get("APNS_AUTH_KEY_P8")
        if (teamId == null || keyId == null || authKey == null) {
            log.warn("APNs VoIP push not configured (APNS_TEAM_ID/APNS_KEY_ID/APNS_AUTH_KEY_P8) — iOS incoming calls fall back to FCM silent push only")
            return@lazy null
        }
        ApnsVoipCredentials(
            teamId = teamId,
            keyId = keyId,
            authKeyP8 = authKey,
            useSandbox = EnvLoader.get("APNS_USE_SANDBOX")?.toBooleanStrictOrNull() ?: false,
        )
    }

    private val bundleIds: Map<ApnsAppTarget, String?> by lazy {
        mapOf(
            ApnsAppTarget.PATIENT to EnvLoader.get("APNS_BUNDLE_ID_PATIENT"),
            ApnsAppTarget.DOCTOR to EnvLoader.get("APNS_BUNDLE_ID_DOCTOR"),
        )
    }

    // APNs provider tokens are valid up to 1 hour — cache and reuse rather than signing per-request.
    @Volatile private var cachedJwt: String? = null
    @Volatile private var cachedJwtIssuedAtMs: Long = 0L
    private val jwtLock = Any()
    private val jwtMaxAgeMs = 50L * 60 * 1000

    private fun loadPrivateKey(p8: String): ECPrivateKey {
        // Env vars holding a pasted multi-line .p8 key often end up with literal "\n" (backslash
        // + n) instead of real newlines. That literal token must be dropped as a whole BEFORE the
        // alphabet filter below — filtering char-by-char keeps the 'n' (it's a normal letter),
        // splicing a spurious 'n' into the middle of the base64 payload and corrupting byte
        // alignment ("Last unit does not have enough valid bits").
        val cleaned = p8
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "")
            .filter { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
        val keyBytes = Base64.getDecoder().decode(cleaned)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECPrivateKey
    }

    private fun currentJwt(creds: ApnsVoipCredentials): String {
        val now = System.currentTimeMillis()
        synchronized(jwtLock) {
            cachedJwt?.let { if (now - cachedJwtIssuedAtMs < jwtMaxAgeMs) return it }
            val algorithm = Algorithm.ECDSA256(null, loadPrivateKey(creds.authKeyP8))
            val token = JWT.create()
                .withIssuer(creds.teamId)
                .withKeyId(creds.keyId)
                .withIssuedAt(Date(now))
                .sign(algorithm)
            cachedJwt = token
            cachedJwtIssuedAtMs = now
            return token
        }
    }

    suspend fun send(
        deviceToken: String,
        event: String,
        data: Map<String, String>,
        target: ApnsAppTarget,
        ttlSeconds: Long? = null,
    ): ApnsSendResult =
        withContext(Dispatchers.IO) {
            val creds = credentials
                ?: return@withContext ApnsSendResult.Error("APNs VoIP push not configured")
            val bundleId = bundleIds[target]
                ?: return@withContext ApnsSendResult.Error("APNS_BUNDLE_ID_${target.name} not configured")
            try {
                val payload = buildJsonObject {
                    put("aps", buildJsonObject { put("content-available", 1) })
                    put("event", JsonPrimitive(event))
                    data.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
                }
                val host = if (creds.useSandbox) "https://api.sandbox.push.apple.com" else "https://api.push.apple.com"
                val requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("$host/3/device/$deviceToken"))
                    .version(HttpClient.Version.HTTP_2)
                    .header("authorization", "bearer ${currentJwt(creds)}")
                    .header("apns-topic", "$bundleId.voip")
                    .header("apns-push-type", "voip")
                    .header("apns-priority", "10")
                // Without this, APNs holds an undeliverable VoIP push (device offline) and can
                // deliver it whenever the device next reconnects — long after the caller gave up,
                // ringing the callee for a call that's already dead. 0 tells APNs to attempt
                // delivery once and discard immediately rather than store it at all.
                if (ttlSeconds != null) {
                    requestBuilder.header("apns-expiration", (System.currentTimeMillis() / 1000 + ttlSeconds).toString())
                }
                val request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(JsonObject.serializer(), payload)))
                    .build()

                val response = http.send(request, HttpResponse.BodyHandlers.ofString())
                when {
                    response.statusCode() == 200 -> ApnsSendResult.Success
                    // Per Apple's guidance: BadDeviceToken (400) and Unregistered (410) mean the
                    // token is permanently dead — the caller should stop storing/using it rather
                    // than retrying it on every future call.
                    response.statusCode() == 410 || response.body().contains("BadDeviceToken") -> {
                        log.warn("APNs VoIP push rejected dead token status=${response.statusCode()} body=${response.body()}")
                        ApnsSendResult.InvalidToken
                    }
                    else -> {
                        log.warn("APNs VoIP push failed status=${response.statusCode()} body=${response.body()}")
                        ApnsSendResult.Error("APNs VoIP push failed: HTTP ${response.statusCode()}")
                    }
                }
            } catch (e: Exception) {
                log.error("APNs VoIP push error — ${e.message}", e)
                ApnsSendResult.Error(e.message ?: "APNs VoIP push failed")
            }
        }
}
