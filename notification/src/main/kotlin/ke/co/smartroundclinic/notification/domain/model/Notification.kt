package ke.co.smartroundclinic.notification.domain.model

import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val channel: NotificationChannel,
    val destination: NotificationDestination,
    val recipientId: String?,
    val status: NotificationStatus,
    val createdAt: String,
    val readAt: String?,
)
