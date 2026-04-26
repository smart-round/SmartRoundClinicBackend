package ke.co.smartroundclinic.notification.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.data.entity.NotificationEntity
import ke.co.smartroundclinic.notification.domain.model.NotificationStatus
import ke.co.smartroundclinic.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import kotlin.time.Clock

class NotificationRepositoryImpl(database: MongoDatabase) : NotificationRepository, NotificationSender {

    private val collection = database.getCollection<NotificationEntity>(MongoDBConstants.NOTIFICATIONS)

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
            // Targeted (recipientId matches) OR broadcast (no recipientId, destination matches role)
            val filter = Filters.or(
                Filters.eq(NotificationEntity::recipientId.name, userId),
                Filters.and(
                    Filters.eq(NotificationEntity::recipientId.name, null),
                    Filters.eq(NotificationEntity::destination.name, destination.name),
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
    }
}
