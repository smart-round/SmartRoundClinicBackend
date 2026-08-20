package ke.co.smartroundclinic.notification.data.repository

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.SendResponse
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.data.entity.PushNotificationLogEntity
import ke.co.smartroundclinic.notification.domain.model.PushNotificationLog
import ke.co.smartroundclinic.notification.domain.model.PushNotificationLogStatus
import ke.co.smartroundclinic.notification.domain.model.PushNotificationSummary
import ke.co.smartroundclinic.notification.domain.model.UserDeviceToken
import ke.co.smartroundclinic.notification.domain.repository.PushNotificationRepository
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import kotlin.time.Clock

private const val FCM_BATCH_SIZE = 500

class PushNotificationRepositoryImpl(
    database: MongoDatabase,
    private val messaging: FirebaseMessaging?,
    private val deviceTokenRepository: UserDeviceTokenRepository,
) : PushNotificationRepository {

    private val logger = LoggerFactory.getLogger(PushNotificationRepositoryImpl::class.java)
    private val logCollection = database.getCollection<PushNotificationLogEntity>(MongoDBConstants.PUSH_NOTIFICATION_LOGS)

    // FCM reports a dead/uninstalled registration as MessagingErrorCode.UNREGISTERED via the v1
    // API, but some already-persisted log rows in this deployment predate that check and instead
    // stored the legacy HTTP-API error name directly in `exception.message` ("NotRegistered") —
    // so a token that's actually dead can surface either way depending on when the error was hit.
    private fun SendResponse.isDeadTokenError(): Boolean =
        exception?.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
            exception?.message?.contains("NotRegistered", ignoreCase = true) == true

    private suspend fun pruneIfDead(response: SendResponse, token: UserDeviceToken) {
        if (!response.isDeadTokenError()) return
        runCatching { deviceTokenRepository.unregister(token.id, token.userId) }
            .onFailure { logger.error("Failed to prune dead FCM token id=${token.id} userId=${token.userId}", it) }
    }

    override suspend fun send(
        tokens: List<UserDeviceToken>,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Resource<PushNotificationSummary> = withContext(Dispatchers.IO) {
        if (messaging == null) {
            return@withContext Resource.Error("Push notifications not configured (FCM credentials missing)")
        }
        if (tokens.isEmpty()) {
            return@withContext Resource.Error("No device tokens found for the specified target")
        }
        try {
            var totalSent = 0
            var totalFailed = 0
            val fcmNotification = Notification.builder().setTitle(title).setBody(body).build()
            val sentAt = Clock.System.now().toString()

            tokens.chunked(FCM_BATCH_SIZE).forEach { chunk ->
                val multicast = MulticastMessage.builder()
                    .setNotification(fcmNotification)
                    .addAllTokens(chunk.map { it.deviceToken })
                    .putAllData(data)
                    .build()

                val batchResponse = messaging.sendEachForMulticast(multicast)

                chunk.forEachIndexed { i, token ->
                    val response = batchResponse.responses[i]
                    logCollection.insertOne(
                        PushNotificationLogEntity(
                            id = ObjectId().toString(),
                            title = title,
                            body = body,
                            deviceToken = token.deviceToken,
                            userId = token.userId,
                            userType = token.userType.name,
                            status = if (response.isSuccessful) PushNotificationLogStatus.SENT.name
                                     else PushNotificationLogStatus.FAILED.name,
                            messageId = response.messageId,
                            error = response.exception?.message,
                            sentAt = sentAt,
                        )
                    )
                    if (response.isSuccessful) totalSent++ else totalFailed++
                    pruneIfDead(response, token)
                }
            }

            Resource.Success(
                data = PushNotificationSummary(sent = totalSent, failed = totalFailed, total = tokens.size),
                message = "Push notifications dispatched: $totalSent sent, $totalFailed failed out of ${tokens.size}",
            )
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send push notifications")
        }
    }

    override suspend fun sendDataOnly(
        tokens: List<UserDeviceToken>,
        event: String,
        data: Map<String, String>,
        ttlSeconds: Long?,
    ): Resource<PushNotificationSummary> = withContext(Dispatchers.IO) {
        if (messaging == null) {
            return@withContext Resource.Error("Push notifications not configured (FCM credentials missing)")
        }
        if (tokens.isEmpty()) {
            return@withContext Resource.Error("No device tokens found for the specified target")
        }
        try {
            var totalSent = 0
            var totalFailed = 0
            val payload = data + ("event" to event)
            val sentAt = Clock.System.now().toString()

            // Without an explicit TTL, FCM/APNs default to holding an undeliverable message for
            // hours to weeks and delivering it whenever the device next reconnects — for ephemeral
            // call signaling that means a "ringing" push can land long after the caller gave up,
            // ringing the callee's phone for a call that's already dead. ttlSeconds caps that.
            val androidConfigBuilder = AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH)
            val apnsConfigBuilder = ApnsConfig.builder()
                .putHeader("apns-push-type", "background")
                .putHeader("apns-priority", "5")
                .setAps(Aps.builder().setContentAvailable(true).build())
            if (ttlSeconds != null) {
                androidConfigBuilder.setTtl(ttlSeconds * 1000)
                apnsConfigBuilder.putHeader("apns-expiration", (Clock.System.now().epochSeconds + ttlSeconds).toString())
            }

            tokens.chunked(FCM_BATCH_SIZE).forEach { chunk ->
                val multicast = MulticastMessage.builder()
                    .putAllData(payload)
                    .addAllTokens(chunk.map { it.deviceToken })
                    .setAndroidConfig(androidConfigBuilder.build())
                    .setApnsConfig(apnsConfigBuilder.build())
                    .build()

                val batchResponse = messaging.sendEachForMulticast(multicast)

                chunk.forEachIndexed { i, token ->
                    val response = batchResponse.responses[i]
                    logCollection.insertOne(
                        PushNotificationLogEntity(
                            id = ObjectId().toString(),
                            title = event,
                            body = "",
                            deviceToken = token.deviceToken,
                            userId = token.userId,
                            userType = token.userType.name,
                            status = if (response.isSuccessful) PushNotificationLogStatus.SENT.name
                                     else PushNotificationLogStatus.FAILED.name,
                            messageId = response.messageId,
                            error = response.exception?.message,
                            sentAt = sentAt,
                        )
                    )
                    if (response.isSuccessful) totalSent++ else totalFailed++
                    pruneIfDead(response, token)
                }
            }

            Resource.Success(
                data = PushNotificationSummary(sent = totalSent, failed = totalFailed, total = tokens.size),
                message = "Data-only push dispatched: $totalSent sent, $totalFailed failed out of ${tokens.size}",
            )
        } catch (e: Exception) {
            // A whole-batch throw (e.g. malformed multicast) previously left zero rows in
            // push_notification_logs — indistinguishable from "never attempted." Log one row per
            // token so a batch-level failure is as diagnosable as a per-token one.
            val failedAt = Clock.System.now().toString()
            tokens.forEach { token ->
                runCatching {
                    logCollection.insertOne(
                        PushNotificationLogEntity(
                            id = ObjectId().toString(),
                            title = event,
                            body = "",
                            deviceToken = token.deviceToken,
                            userId = token.userId,
                            userType = token.userType.name,
                            status = PushNotificationLogStatus.FAILED.name,
                            error = "Batch send threw: ${e.localizedMessage ?: e.toString()}",
                            sentAt = failedAt,
                        )
                    )
                }
            }
            Resource.Error(e.localizedMessage ?: "Failed to send data-only push notifications")
        }
    }

    override suspend fun getLogs(page: Int, size: Int): Resource<Pair<List<PushNotificationLog>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val total = logCollection.countDocuments()
                val items = logCollection.find()
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items.map { it.toModel() } to total, message = "Logs retrieved")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve push notification logs")
            }
        }
}
