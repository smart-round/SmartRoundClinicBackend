package ke.co.smartroundclinic.notification.domain.model

data class PushNotificationLog(
    val id: String,
    val title: String,
    val body: String,
    val deviceToken: String,
    val userId: String,
    val userType: UserType,
    val status: PushNotificationLogStatus,
    val messageId: String?,
    val error: String?,
    val sentAt: String,
)
