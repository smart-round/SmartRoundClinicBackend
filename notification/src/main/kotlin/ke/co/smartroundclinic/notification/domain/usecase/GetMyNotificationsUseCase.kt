package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.notification.domain.repository.NotificationRepository
import ke.co.smartroundclinic.notification.presentation.dto.response.NotificationPageResult
import ke.co.smartroundclinic.notification.presentation.dto.response.toRes
import kotlin.math.ceil

class GetMyNotificationsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(
        userId: String,
        destination: NotificationDestination,
        page: Int,
        size: Int,
    ): DefaultResponse<NotificationPageResult?> =
        repository.getForUser(userId, destination, page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                NotificationPageResult(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
