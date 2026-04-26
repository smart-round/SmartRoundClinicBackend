package ke.co.smartroundclinic.notification.presentation.dto.request

import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.notification.domain.model.Notification
import ke.co.smartroundclinic.notification.domain.model.NotificationStatus
import org.bson.types.ObjectId
import kotlin.time.Clock

data class CreateNotificationReq(
    val title: String,
    val message: String,
    val channel: NotificationChannel,
    val destination: NotificationDestination,
    val recipientId: String? = null,
) {
    fun toModel() = Notification(
        id = ObjectId().toString(),
        title = title,
        message = message,
        channel = channel,
        destination = destination,
        recipientId = recipientId,
        status = NotificationStatus.UNREAD,
        createdAt = Clock.System.now().toString(),
        readAt = null,
    )
}
