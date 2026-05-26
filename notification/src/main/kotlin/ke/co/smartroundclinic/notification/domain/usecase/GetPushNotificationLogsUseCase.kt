package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.notification.domain.repository.PushNotificationRepository
import ke.co.smartroundclinic.notification.presentation.dto.response.PushNotificationLogPageResult
import ke.co.smartroundclinic.notification.presentation.dto.response.toRes
import kotlin.math.ceil

class GetPushNotificationLogsUseCase(private val repository: PushNotificationRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<PushNotificationLogPageResult?> =
        repository.getLogs(page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                PushNotificationLogPageResult(
                    items = items.map { it.toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
