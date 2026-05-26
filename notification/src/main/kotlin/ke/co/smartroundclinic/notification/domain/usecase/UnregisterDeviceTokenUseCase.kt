package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import ke.co.smartroundclinic.notification.presentation.dto.response.UserDeviceTokenRes
import ke.co.smartroundclinic.notification.presentation.dto.response.toRes

class UnregisterDeviceTokenUseCase(private val repository: UserDeviceTokenRepository) {
    suspend operator fun invoke(tokenId: String, userId: String): DefaultResponse<UserDeviceTokenRes?> =
        repository.unregister(tokenId, userId)
            .toDefaultResponse(successStatusCode = 200, failedStatusCode = 404) { it?.toRes() }
}
