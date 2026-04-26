package ke.co.smartroundclinic.notification.presentation.dto.response

import ke.co.smartroundclinic.notification.domain.model.Notification
import kotlinx.serialization.Serializable

@Serializable
data class NotificationRes(
    val id: String,
    val title: String,
    val message: String,
    val channel: String,
    val destination: String,
    val recipientId: String?,
    val status: String,
    val createdAt: String,
    val readAt: String?,
)

@Serializable
data class NotificationPageResult(
    val items: List<NotificationRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun Notification.toRes() = NotificationRes(
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
