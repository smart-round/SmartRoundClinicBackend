package ke.co.smartroundclinic.common

interface NotificationSender {
    suspend fun send(
        title: String,
        message: String,
        channel: NotificationChannel,
        destination: NotificationDestination,
        recipientId: String? = null,
        metadata: Map<String, String> = emptyMap(),
    )

    /**
     * Ephemeral call-signaling push (invite/answer/decline/cancel) — data-only and not persisted
     * to the in-app notification inbox, unlike [send]. See PushNotificationRepository.sendDataOnly.
     */
    suspend fun sendCallSignal(
        event: String,
        recipientId: String,
        metadata: Map<String, String> = emptyMap(),
    )
}
