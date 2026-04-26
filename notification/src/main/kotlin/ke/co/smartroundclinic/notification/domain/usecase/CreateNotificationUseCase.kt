package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.notification.data.entity.toEntity
import ke.co.smartroundclinic.notification.domain.repository.NotificationRepository
import ke.co.smartroundclinic.notification.presentation.dto.request.CreateNotificationReq
import ke.co.smartroundclinic.notification.presentation.dto.response.NotificationRes
import ke.co.smartroundclinic.notification.presentation.dto.response.toRes

class CreateNotificationUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(req: CreateNotificationReq): DefaultResponse<NotificationRes?> =
        repository.create(req.toModel().toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { it?.toModel()?.toRes() }
}
