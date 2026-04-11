package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.model.User
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadProfilePictureUseCase(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        userId: String,
        imageBytes: ByteArray,
        contentType: String,
    ): DefaultResponse<UserRes?> = withContext(Dispatchers.IO) {

        val extension = when {
            contentType.contains("png") -> "png"
            contentType.contains("jpeg") -> "jpeg"
            contentType.contains("webp") -> "webp"
            contentType.contains("gif") -> "gif"
            else -> "jpg"
        }

        val key = "profile-pictures/$userId.$extension"

        val uploadResult = storageRepository.upload(
            bucket = BUCKET,
            key = key,
            content = imageBytes,
            contentType = contentType,
        )

        if (uploadResult is Resource.Error) return@withContext uploadResult.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Failed to upload profile picture"
        ) { null }

        val url = uploadResult.data ?: return@withContext Resource.Error<UserRes?>(
            message = "Upload returned no URL"
        ).toDefaultResponse(failedStatusCode = HttpStatusCode.InternalServerError.value) { null }

        val updateResult = userRepository.updateProfilePicture(userId = userId, url = url)

        if (updateResult is Resource.Error) return@withContext updateResult.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Picture uploaded but failed to save URL"
        ) { null }

        userRepository.getUser(userId).toDefaultResponse(
            successStatusCode = HttpStatusCode.OK.value,
            successMessage = "Profile picture updated successfully"
        ) { it?.toModel()?.toUserRes() }
    }

    companion object {
        private const val BUCKET = "user-profiles"
    }
}
