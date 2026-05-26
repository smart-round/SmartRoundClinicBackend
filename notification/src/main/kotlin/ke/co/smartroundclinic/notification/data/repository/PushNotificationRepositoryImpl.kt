package ke.co.smartroundclinic.notification.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import kotlin.time.Clock

private const val FCM_BATCH_SIZE = 500

class PushNotificationRepositoryImpl(
    database: MongoDatabase,
    private val messaging: FirebaseMessaging,
) : PushNotificationRepository {

    private val logCollection = database.getCollection<PushNotificationLogEntity>(MongoDBConstants.PUSH_NOTIFICATION_LOGS)

    override suspend fun send(
        tokens: List<UserDeviceToken>,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Resource<PushNotificationSummary> = withContext(Dispatchers.IO) {
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
