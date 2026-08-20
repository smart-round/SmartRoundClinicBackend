package ke.co.smartroundclinic.notification.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.domain.model.PushNotificationLog
import ke.co.smartroundclinic.notification.domain.model.PushNotificationSummary
import ke.co.smartroundclinic.notification.domain.model.UserDeviceToken

interface PushNotificationRepository {
    suspend fun send(
        tokens: List<UserDeviceToken>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): Resource<PushNotificationSummary>

    /**
     * Data-only, high-priority push with no visible title/body — used for ephemeral signaling
     * (call invite/answer/decline/cancel) where app code must run even while backgrounded, rather
     * than letting the OS auto-display a tray notification. On iOS this is a best-effort silent
     * (content-available) push, which the OS may throttle or drop when the app is fully killed —
     * a genuine VoIP push (see VoipPushSender) is the reliable channel for that case.
     */
    suspend fun sendDataOnly(
        tokens: List<UserDeviceToken>,
        event: String,
        data: Map<String, String> = emptyMap(),
        ttlSeconds: Long? = null,
    ): Resource<PushNotificationSummary>

    suspend fun getLogs(page: Int, size: Int): Resource<Pair<List<PushNotificationLog>, Long>>
}
