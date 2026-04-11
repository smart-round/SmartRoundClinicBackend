package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class RemoveProfilePictureUseCase(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(userId: String): DefaultResponse<UserRes?> =
        withContext(Dispatchers.IO) {
            supervisorScope {
                val userResult = userRepository.getUser(userId)
                val user = userResult.data
                    ?: return@supervisorScope userResult.toDefaultResponse(
                        failedStatusCode = HttpStatusCode.NotFound.value,
                        errorMessage = "User not found"
                    ) { null }

                // Delete the object from R2 fire-and-forget; a storage failure should not
                // prevent clearing the reference on the user record.
                user.profilePicture?.let { url ->
                    launch {
                        val key = "profile-pictures/$userId.${url.substringAfterLast(".")}"
                        storageRepository.delete(bucket = BUCKET, key = key)
                    }
                }

                userRepository.updateProfilePicture(userId = userId, url = null)
                    .let { updateResult ->
                        if (updateResult is Resource.Error) return@supervisorScope updateResult.toDefaultResponse(
                            failedStatusCode = HttpStatusCode.InternalServerError.value,
                            errorMessage = "Failed to remove profile picture"
                        ) { null }
                    }

                userRepository.getUser(userId).toDefaultResponse(
                    successStatusCode = HttpStatusCode.OK.value,
                    successMessage = "Profile picture removed successfully"
                ) { it?.toModel()?.toUserRes() }
            }
        }

    companion object {
        private const val BUCKET = "user-profiles"
    }
}
