package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.presentation.dto.response.UserRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
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

        // Content type is already validated by the routing layer before this use case is called.
        val extension = imageExtensionOrNull(contentType) ?: "jpeg"

        val key = "profile-pictures/$userId.$extension"

        val uploadResult = storageRepository.upload(
            bucket = AppConfig.r2.bucket,
            key = key,
            content = imageBytes,
            contentType = contentType,
        )

        if (uploadResult is Resource.Error) return@withContext uploadResult.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Failed to upload profile picture"
        ) { null }

        val uploadedKey = uploadResult.data ?: return@withContext Resource.Error<UserRes?>(
            message = "Upload returned no key"
        ).toDefaultResponse(failedStatusCode = HttpStatusCode.InternalServerError.value) { null }

        // Store the key (not a URL) in the database.
        val updateResult = userRepository.updateProfilePicture(userId = userId, url = uploadedKey)

        if (updateResult is Resource.Error) return@withContext updateResult.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Picture uploaded but failed to save reference"
        ) { null }

        // Generate a presigned URL (24 h) for the response.
        val presignResult = storageRepository.presignedGetUrl(
            bucket = AppConfig.r2.bucket,
            key = uploadedKey,
            expiresInSeconds = 86400,
        )
        val presignedUrl = (presignResult as? Resource.Success)?.data

        userRepository.getUser(userId).toDefaultResponse(
            successStatusCode = HttpStatusCode.OK.value,
            successMessage = "Profile picture updated successfully"
        ) { entity ->
            entity?.toModel()?.toUserRes()?.copy(profilePicture = presignedUrl)
        }
    }
}
