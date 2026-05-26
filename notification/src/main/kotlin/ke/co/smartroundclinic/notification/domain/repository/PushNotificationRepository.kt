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

    suspend fun getLogs(page: Int, size: Int): Resource<Pair<List<PushNotificationLog>, Long>>
}
