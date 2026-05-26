package ke.co.smartroundclinic.notification.presentation.dto.request

import ke.co.smartroundclinic.common.NotificationDestination
import kotlinx.serialization.Serializable

@Serializable
data class SendPushNotificationReq(
    val title: String,
    val body: String,
    val recipientId: String? = null,
    val destination: NotificationDestination? = null,
    val data: Map<String, String> = emptyMap(),
)
