package ke.co.smartroundclinic.notification.data.entity

import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.notification.domain.model.Notification
import ke.co.smartroundclinic.notification.domain.model.NotificationStatus
import org.bson.types.ObjectId
import kotlin.time.Clock

data class NotificationEntity(
    val id: String = ObjectId().toString(),
    val title: String,
    val message: String,
    val channel: String,
    val destination: String,
    val recipientId: String? = null,
    val status: String = NotificationStatus.UNREAD.name,
    val createdAt: String = Clock.System.now().toString(),
    val readAt: String? = null,
) {
    fun toModel() = Notification(
        id = id,
        title = title,
        message = message,
        channel = runCatching { NotificationChannel.valueOf(channel) }.getOrDefault(NotificationChannel.IN_APP),
        destination = runCatching { NotificationDestination.valueOf(destination) }.getOrDefault(NotificationDestination.DOCTOR),
        recipientId = recipientId,
        status = runCatching { NotificationStatus.valueOf(status) }.getOrDefault(NotificationStatus.UNREAD),
        createdAt = createdAt,
        readAt = readAt,
    )
}

fun Notification.toEntity() = NotificationEntity(
    id = id,
    title = title,
    message = message,
    channel = channel.name,
    destination = destination.name,
    recipientId = recipientId,
    status = status.name,
    createdAt = createdAt,
    readAt = readAt,
)
