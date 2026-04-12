package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetUserUserUseCase(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(userId: String): DefaultResponse<UserRes?> =
        withContext(Dispatchers.IO) {
            val userResult = userRepository.getUser(userId)

            // Generate the presigned URL before entering the non-suspend toDefaultResponse lambda.
            val presignedUrl = userResult.data?.profilePicture?.let { key ->
                val result = storageRepository.presignedGetUrl(
                    bucket = AppConfig.r2.bucket,
                    key = key,
                    expiresInSeconds = 86400,
                )
                (result as? Resource.Success)?.data
            }

            userResult.toDefaultResponse { entity ->
                entity?.toModel()?.toUserRes()?.copy(profilePicture = presignedUrl)
            }
        }
}
