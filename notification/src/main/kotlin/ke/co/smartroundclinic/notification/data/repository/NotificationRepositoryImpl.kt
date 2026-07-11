package ke.co.smartroundclinic.notification.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.apns.ApnsAppTarget
import ke.co.smartroundclinic.infra.apns.ApnsVoipClient
import ke.co.smartroundclinic.notification.data.entity.NotificationEntity
import ke.co.smartroundclinic.notification.domain.model.NotificationStatus
import ke.co.smartroundclinic.notification.domain.model.TokenType
import ke.co.smartroundclinic.notification.domain.model.UserType
import ke.co.smartroundclinic.notification.domain.repository.NotificationRepository
import ke.co.smartroundclinic.notification.domain.repository.PushNotificationRepository
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class NotificationRepositoryImpl(
    database: MongoDatabase,
    private val deviceTokenRepo: UserDeviceTokenRepository,
    private val pushRepo: PushNotificationRepository,
    private val apnsVoipClient: ApnsVoipClient? = null,
) : NotificationRepository, NotificationSender {

    private val collection = database.getCollection<NotificationEntity>(MongoDBConstants.NOTIFICATIONS)
    private val logger = LoggerFactory.getLogger(NotificationRepositoryImpl::class.java)

    override suspend fun create(entity: NotificationEntity): Resource<NotificationEntity?> =
        withContext(Dispatchers.IO) {
            try {
                collection.insertOne(entity)
                Resource.Success(data = entity, message = "Notification sent successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to send notification")
            }
        }

    override suspend fun getById(id: String): Resource<NotificationEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(NotificationEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Notification not found")
                Resource.Success(data = entity, message = "Notification retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve notification")
            }
        }

    override suspend fun getForUser(
        userId: String,
        destination: NotificationDestination,
        page: Int,
        size: Int,
    ): Resource<Pair<List<NotificationEntity>, Long>> = withContext(Dispatchers.IO) {
        try {
            val safePage = maxOf(1, page)
            val safeSize = minOf(maxOf(1, size), 100)
            // Targeted (recipientId matches) OR broadcast matching role or ALL destination
            val filter = Filters.or(
                Filters.eq(NotificationEntity::recipientId.name, userId),
                Filters.and(
                    Filters.eq(NotificationEntity::recipientId.name, null),
                    Filters.`in`(
                        NotificationEntity::destination.name,
                        destination.name,
                        NotificationDestination.ALL.name,
                    ),
                ),
            )
            val total = collection.countDocuments(filter)
            val items = collection.find(filter)
                .skip((safePage - 1) * safeSize)
                .limit(safeSize)
                .toList()
            Resource.Success(data = items to total, message = "Notifications retrieved successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to retrieve notifications")
        }
    }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<NotificationEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val total = collection.countDocuments()
                val items = collection.find()
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Notifications retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve notifications")
            }
        }

    override suspend fun markAsRead(
        id: String,
        callerId: String,
        callerDestination: NotificationDestination,
    ): Resource<NotificationEntity?> = withContext(Dispatchers.IO) {
        try {
            val existing = collection.find(Filters.eq(NotificationEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Notification not found")

            // Targeted: caller must be the recipient
            // Broadcast: caller's destination must match the notification's destination
            val authorized = when {
                existing.recipientId != null -> existing.recipientId == callerId
                else -> existing.destination == callerDestination.name
            }
            if (!authorized)
                return@withContext Resource.Error("You are not authorized to mark this notification as read")

            if (existing.status == NotificationStatus.READ.name)
                return@withContext Resource.Error("Notification is already read")

            collection.updateOne(
                Filters.eq(NotificationEntity::id.name, id),
                Updates.combine(
                    Updates.set(NotificationEntity::status.name, NotificationStatus.READ.name),
                    Updates.set(NotificationEntity::readAt.name, Clock.System.now().toString()),
                )
            )
            val updated = collection.find(Filters.eq(NotificationEntity::id.name, id)).firstOrNull()
            Resource.Success(data = updated, message = "Notification marked as read")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark notification as read")
        }
    }

    override suspend fun delete(id: String): Resource<NotificationEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(NotificationEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Notification not found")
                collection.deleteOne(Filters.eq(NotificationEntity::id.name, id))
                Resource.Success(data = existing, message = "Notification deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete notification")
            }
        }

    // NotificationSender — called by other modules; null recipientId = broadcast to destination
    override suspend fun send(
        title: String,
        message: String,
        channel: NotificationChannel,
        destination: NotificationDestination,
        recipientId: String?,
        metadata: Map<String, String>,
    ) {
        create(
            NotificationEntity(
                id = ObjectId().toString(),
                title = title,
                message = message,
                channel = channel.name,
                destination = destination.name,
                recipientId = recipientId,
            )
        )

        if (channel == NotificationChannel.PUSH_NOTIFICATION) {
            val tokensResource = if (recipientId != null) {
                deviceTokenRepo.getByUser(recipientId)
            } else {
                when (destination) {
                    NotificationDestination.DOCTOR -> deviceTokenRepo.getByUserType(UserType.DOCTOR)
                    NotificationDestination.PATIENT -> deviceTokenRepo.getByUserType(UserType.PATIENT)
                    NotificationDestination.ALL -> deviceTokenRepo.getAll()
                    else -> Resource.Success(emptyList())
                }
            }
            val tokens = (tokensResource as? Resource.Success)?.data ?: emptyList()
            if (tokens.isNotEmpty()) {
                runCatching { pushRepo.send(tokens, title, message, metadata) }
            }
        }
    }

    override suspend fun sendCallSignal(
        event: String,
        recipientId: String,
        metadata: Map<String, String>,
    ) {
        val tokensResource = deviceTokenRepo.getByUser(recipientId)
        val tokens = (tokensResource as? Resource.Success)?.data ?: emptyList()
        val (voipTokens, standardTokens) = tokens.partition { it.tokenType == TokenType.VOIP }
        logger.info(
            "sendCallSignal event=$event recipientId=$recipientId " +
                "getByUser=${if (tokensResource is Resource.Error) "ERROR:${tokensResource.message}" else "OK"} " +
                "totalTokens=${tokens.size} standard=${standardTokens.size} voip=${voipTokens.size} " +
                "apnsVoipClientConfigured=${apnsVoipClient != null}"
        )

        if (standardTokens.isNotEmpty()) {
            runCatching { pushRepo.sendDataOnly(standardTokens, event, metadata) }
                .onFailure { logger.error("sendCallSignal: sendDataOnly threw for event=$event recipientId=$recipientId", it) }
                .onSuccess { result ->
                    if (result is Resource.Error) {
                        logger.warn("sendCallSignal: sendDataOnly returned error for event=$event recipientId=$recipientId: ${result.message}")
                    }
                }
        }
        // VoIP tokens ring reliably via a direct APNs push even when the app is killed — the
        // reliable channel for iOS, on top of (not instead of) the FCM silent-push fallback above.
        val voipClient = apnsVoipClient
        if (voipTokens.isNotEmpty() && voipClient != null) {
            voipTokens.forEach { token ->
                val target = when (token.userType) {
                    UserType.DOCTOR -> ApnsAppTarget.DOCTOR
                    UserType.PATIENT -> ApnsAppTarget.PATIENT
                }
                runCatching { voipClient.send(token.deviceToken, event, metadata, target) }
                    .onFailure { logger.error("sendCallSignal: voipClient.send threw for event=$event recipientId=$recipientId", it) }
            }
        }
    }
}
