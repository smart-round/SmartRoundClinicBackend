package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.notification.domain.repository.NotificationRepository
import ke.co.smartroundclinic.notification.presentation.dto.response.NotificationRes
import ke.co.smartroundclinic.notification.presentation.dto.response.toRes

class GetNotificationByIdUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<NotificationRes?> =
        repository.getById(id).toDefaultResponse { it?.toModel()?.toRes() }
}
