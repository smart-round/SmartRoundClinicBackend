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
     *
     * [ttlSeconds] caps how long the push transport (FCM/APNs) may hold this message undelivered
     * before dropping it — without it, a callee whose device is offline can have a stale "Incoming
     * Video Call" push queue and arrive minutes later, well after the caller gave up, ringing the
     * phone for a call that no longer exists. Callers should pass the same window the invite is
     * valid for (RedisKeys.CALL_INVITE_TTL_SECONDS) so a push that can't matter anymore is never
     * delivered at all.
     */
    suspend fun sendCallSignal(
        event: String,
        recipientId: String,
        metadata: Map<String, String> = emptyMap(),
        ttlSeconds: Long? = null,
    )
}
