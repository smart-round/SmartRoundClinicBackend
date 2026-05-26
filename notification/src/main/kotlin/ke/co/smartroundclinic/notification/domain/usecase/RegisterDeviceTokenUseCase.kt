package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import ke.co.smartroundclinic.notification.presentation.dto.request.RegisterDeviceTokenReq
import ke.co.smartroundclinic.notification.presentation.dto.response.UserDeviceTokenRes
import ke.co.smartroundclinic.notification.presentation.dto.response.toRes

class RegisterDeviceTokenUseCase(private val repository: UserDeviceTokenRepository) {
    suspend operator fun invoke(req: RegisterDeviceTokenReq, userId: String, userType: String): DefaultResponse<UserDeviceTokenRes?> =
        repository.register(req.toModel(userId, userType))
            .toDefaultResponse(successStatusCode = 200, failedStatusCode = 400) { it?.toRes() }
}
