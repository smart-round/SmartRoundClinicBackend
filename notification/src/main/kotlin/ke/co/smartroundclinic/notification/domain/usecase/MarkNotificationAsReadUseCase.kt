package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.notification.domain.repository.NotificationRepository
import ke.co.smartroundclinic.notification.presentation.dto.response.NotificationRes
import ke.co.smartroundclinic.notification.presentation.dto.response.toRes

class MarkNotificationAsReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(
        id: String,
        callerId: String,
        callerDestination: NotificationDestination,
    ): DefaultResponse<NotificationRes?> =
        repository.markAsRead(id, callerId, callerDestination).toDefaultResponse { it?.toModel()?.toRes() }
}
