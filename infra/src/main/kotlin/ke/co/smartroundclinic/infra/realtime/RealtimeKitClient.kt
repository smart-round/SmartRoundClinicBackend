package ke.co.smartroundclinic.infra.realtime

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

/**
 * Thin wrapper over Cloudflare's RealtimeKit REST API.
 *
 * Docs:
 *  - https://developers.cloudflare.com/api/resources/realtime_kit/subresources/meetings
 *  - https://developers.cloudflare.com/api/resources/realtime_kit/subresources/meetings/subresources/participants
 */
class RealtimeKitClient : KoinComponent {

    private val http: HttpClient by inject()
    private val log = LoggerFactory.getLogger(RealtimeKitClient::class.java)

    private val baseMeetingsPath: String
        get() = "${AppConfig.realtimeKit.baseUrl.trimEnd('/')}/accounts/${AppConfig.realtimeKit.accountId}/realtime/kit/${AppConfig.realtimeKit.appId}/meetings"

    /** Creates a new meeting. Returns the meeting id (`data.id`). */
    suspend fun createMeeting(title: String, preferredRegion: String? = null): Resource<String> = try {
        log.info("Cloudflare RTK createMeeting -> $baseMeetingsPath")
        val payload = outgoingJson.encodeToString(
            CreateMeetingReq.serializer(),
            CreateMeetingReq(title = title, preferredRegion = preferredRegion),
        )
        val res: HttpResponse = http.post(baseMeetingsPath) {
            bearerAuth(AppConfig.realtimeKit.apiToken)
            contentType(ContentType.Application.Json)
            headers { append(HttpHeaders.Accept, "application/json") }
            setBody(TextContent(payload, ContentType.Application.Json))
            timeout {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 15_000
            }
        }
        val raw = res.bodyAsText()
        if (!res.status.isSuccess()) {
            log.error("Cloudflare RTK createMeeting failed status=${res.status.value} body=$raw")
            Resource.Error("Cloudflare RealtimeKit createMeeting ${res.status.value}: $raw")
        } else {
            val body = jsonLenient.decodeFromString(CloudflareEnvelope.serializer(MeetingData.serializer()), raw)
            val id = body.data?.id
            if (body.success && !id.isNullOrBlank()) Resource.Success(id)
            else Resource.Error("Cloudflare RealtimeKit returned no meeting id: $raw")
        }
    } catch (e: Exception) {
        log.error("Cloudflare RTK createMeeting threw — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to create RealtimeKit meeting")
    }

    /**
     * Lists all meetings in this app and returns the id of the first LIVE meeting
     * whose title exactly matches [title], or null if none is found.
     *
     * Used to detect whether a meeting for this consultation already exists on
     * Cloudflare before creating a new one.
     */
    suspend fun findActiveMeetingByTitle(title: String): String? = try {
        log.info("Cloudflare RTK listMeetings searching for title=\"$title\"")
        val res: HttpResponse = http.get(baseMeetingsPath) {
            bearerAuth(AppConfig.realtimeKit.apiToken)
            headers { append(HttpHeaders.Accept, "application/json") }
            timeout {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 10_000
            }
        }
        if (!res.status.isSuccess()) {
            log.warn("Cloudflare RTK listMeetings failed status=${res.status.value}")
            null
        } else {
            val raw = res.bodyAsText()
            val body = jsonLenient.decodeFromString(CloudflareMeetingListEnvelope.serializer(), raw)
            body.data
                .firstOrNull { it.title == title && it.status != null && it.status != "INACTIVE" }
                ?.id
                .also { if (it != null) log.info("Cloudflare RTK found ACTIVE meeting id=$it for title=\"$title\"") }
        }
    } catch (e: Exception) {
        log.warn("Cloudflare RTK listMeetings threw — ${e.message}")
        null
    }

    /** Returns the current status of a meeting ("LIVE", "INACTIVE", etc.), or null if not found. */
    suspend fun getMeetingStatus(meetingId: String): String? = try {
        log.info("Cloudflare RTK getMeeting -> $baseMeetingsPath/$meetingId")
        val res: HttpResponse = http.get("$baseMeetingsPath/$meetingId") {
            bearerAuth(AppConfig.realtimeKit.apiToken)
            headers { append(HttpHeaders.Accept, "application/json") }
            timeout {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 10_000
            }
        }
        val raw = res.bodyAsText()
        if (!res.status.isSuccess()) null
        else {
            val body = jsonLenient.decodeFromString(CloudflareEnvelope.serializer(MeetingStatusData.serializer()), raw)
            body.data?.status
        }
    } catch (e: Exception) {
        log.warn("Cloudflare RTK getMeeting threw — ${e.message}")
        null
    }

    /** Ends an active meeting on Cloudflare. Participants are kicked out immediately. */
    suspend fun endMeeting(meetingId: String): Resource<Unit> = try {
        log.info("Cloudflare RTK endMeeting -> $baseMeetingsPath/$meetingId")
        val res: HttpResponse = http.delete("$baseMeetingsPath/$meetingId") {
            bearerAuth(AppConfig.realtimeKit.apiToken)
            headers { append(HttpHeaders.Accept, "application/json") }
            timeout {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 15_000
            }
        }
        val raw = res.bodyAsText()
        if (!res.status.isSuccess()) {
            log.error("Cloudflare RTK endMeeting failed status=${res.status.value} body=$raw")
            Resource.Error("Cloudflare RealtimeKit endMeeting ${res.status.value}: $raw")
        } else {
            log.info("Cloudflare RTK endMeeting succeeded meeting=$meetingId")
            Resource.Success(Unit)
        }
    } catch (e: Exception) {
        log.error("Cloudflare RTK endMeeting threw — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to end RealtimeKit meeting")
    }

    /** Adds a participant to the given meeting and returns the join auth token. */
    suspend fun addParticipant(
        meetingId: String,
        customParticipantId: String,
        presetName: String,
        name: String?,
        picture: String?,
    ): Resource<ParticipantData> = try {
        log.info("Cloudflare RTK addParticipant meeting=$meetingId user=$customParticipantId preset=$presetName")
        val payload = outgoingJson.encodeToString(
            AddParticipantReq.serializer(),
            AddParticipantReq(
                customParticipantId = customParticipantId,
                presetName = presetName,
                name = name?.takeIf { it.isNotBlank() },
                picture = picture?.takeIf { it.isNotBlank() },
            ),
        )
        val res: HttpResponse = http.post("$baseMeetingsPath/$meetingId/participants") {
            bearerAuth(AppConfig.realtimeKit.apiToken)
            contentType(ContentType.Application.Json)
            headers { append(HttpHeaders.Accept, "application/json") }
            setBody(TextContent(payload, ContentType.Application.Json))
            timeout {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 15_000
            }
        }
        val raw = res.bodyAsText()
        if (!res.status.isSuccess()) {
            log.error("Cloudflare RTK addParticipant failed status=${res.status.value} body=$raw")
            Resource.Error("Cloudflare RealtimeKit addParticipant ${res.status.value}: $raw")
        } else {
            val body = jsonLenient.decodeFromString(CloudflareEnvelope.serializer(ParticipantData.serializer()), raw)
            val data = body.data
            if (body.success && data != null && data.token.isNotBlank()) Resource.Success(data)
            else Resource.Error("Cloudflare RealtimeKit returned no participant token: $raw")
        }
    } catch (e: Exception) {
        log.error("Cloudflare RTK addParticipant threw — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to add RealtimeKit participant")
    }

    private fun HttpStatusCode.isSuccess() = value in 200..299

    private val jsonLenient = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /** Outgoing requests must NOT serialise `null` fields — Cloudflare validates
     *  `picture` as a URI and rejects literal nulls with HTTP 400. */
    private val outgoingJson = kotlinx.serialization.json.Json {
        explicitNulls = false
        encodeDefaults = false
    }
}

@Serializable
internal data class CloudflareEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val errors: List<CloudflareError> = emptyList(),
    val messages: List<CloudflareMessage> = emptyList(),
)

@Serializable
internal data class CloudflareError(val code: Int? = null, val message: String? = null)

@Serializable
internal data class CloudflareMessage(val code: Int? = null, val message: String? = null)

@Serializable
internal data class CreateMeetingReq(
    val title: String,
    @SerialName("preferred_region") val preferredRegion: String? = null,
)

@Serializable
internal data class MeetingData(val id: String)

@Serializable
internal data class MeetingStatusData(val id: String, val status: String? = null)

@Serializable
internal data class MeetingListItem(
    val id: String,
    val title: String? = null,
    val status: String? = null,
)

@Serializable
internal data class CloudflareMeetingListEnvelope(
    val success: Boolean = false,
    val data: List<MeetingListItem> = emptyList(),
)

@Serializable
internal data class AddParticipantReq(
    @SerialName("custom_participant_id") val customParticipantId: String,
    @SerialName("preset_name") val presetName: String,
    val name: String? = null,
    val picture: String? = null,
)

@Serializable
data class ParticipantData(
    val id: String,
    val token: String,
    @SerialName("custom_participant_id") val customParticipantId: String,
    // Cloudflare actually returns `preset_id`, not `preset_name`, in the
    // addParticipant response — both are kept optional so deserialization
    // never depends on which one shows up.
    @SerialName("preset_name") val presetName: String? = null,
    @SerialName("preset_id") val presetId: String? = null,
    val name: String? = null,
    val picture: String? = null,
)
