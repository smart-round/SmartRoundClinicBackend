package ke.co.smartroundclinic.common

interface NotificationSender {
    suspend fun send(
        title: String,
        message: String,
        channel: NotificationChannel,
        destination: NotificationDestination,
        recipientId: String? = null,
    )
}
