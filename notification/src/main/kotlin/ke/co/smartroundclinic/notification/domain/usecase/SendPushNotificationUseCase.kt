package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.domain.model.PushNotificationSummary
import ke.co.smartroundclinic.notification.domain.model.UserType
import ke.co.smartroundclinic.notification.domain.repository.PushNotificationRepository
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import ke.co.smartroundclinic.notification.presentation.dto.request.SendPushNotificationReq
import ke.co.smartroundclinic.notification.presentation.dto.response.PushNotificationSummaryRes

class SendPushNotificationUseCase(
    private val deviceTokenRepo: UserDeviceTokenRepository,
    private val pushRepo: PushNotificationRepository,
) {
    suspend operator fun invoke(req: SendPushNotificationReq): DefaultResponse<PushNotificationSummaryRes?> {
        if (req.recipientId == null && req.destination == null)
            return Resource.Error<PushNotificationSummary>("Either recipientId or destination must be specified")
                .toDefaultResponse(failedStatusCode = 400) { null }

        if (req.recipientId != null && req.destination != null)
            return Resource.Error<PushNotificationSummary>("Specify either recipientId or destination, not both")
                .toDefaultResponse(failedStatusCode = 400) { null }

        val tokensResult = when {
            req.recipientId != null -> deviceTokenRepo.getByUser(req.recipientId)
            req.destination == NotificationDestination.DOCTOR -> deviceTokenRepo.getByUserType(UserType.DOCTOR)
            req.destination == NotificationDestination.PATIENT -> deviceTokenRepo.getByUserType(UserType.PATIENT)
            else -> deviceTokenRepo.getAll()
        }

        val tokens = when (tokensResult) {
            is Resource.Success -> tokensResult.data ?: emptyList()
            is Resource.Error -> return Resource.Error<PushNotificationSummary>(tokensResult.message ?: "Failed to retrieve device tokens")
                .toDefaultResponse(failedStatusCode = 500) { null }
        }

        return pushRepo.send(tokens, req.title, req.body, req.data)
            .toDefaultResponse(successStatusCode = 200, failedStatusCode = 500) { summary ->
                summary?.let { PushNotificationSummaryRes(sent = it.sent, failed = it.failed, total = it.total) }
            }
    }
}
