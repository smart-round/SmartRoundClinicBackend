package ke.co.smartroundclinic.admin.domain.usecase.speciality

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.SpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadSpecialityIconUseCase(
    private val specialityRepository: SpecialityRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        id: String,
        imageBytes: ByteArray,
        contentType: String,
    ): DefaultResponse<SpecialityRes?> = withContext(Dispatchers.IO) {
        val extension = imageExtensionOrNull(contentType) ?: "jpeg"
        val key = "speciality-icons/$id.$extension"

        val uploadResult = storageRepository.upload(
            bucket = AppConfig.r2.bucket,
            key = key,
            content = imageBytes,
            contentType = contentType,
        )

        if (uploadResult is Resource.Error) return@withContext uploadResult.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Failed to upload icon"
        ) { null }

        val presignResult = storageRepository.presignedGetUrl(
            bucket = AppConfig.r2.bucket,
            key = key,
            expiresInSeconds = ICON_URL_TTL,
        )

        val presignedUrl = presignResult.data ?: return@withContext Resource.Error<SpecialityRes?>(
            message = "Failed to generate icon URL"
        ).toDefaultResponse(failedStatusCode = HttpStatusCode.InternalServerError.value) { null }

        specialityRepository.updateSpecialityIcon(id, presignedUrl)
            .toDefaultResponse(
                successStatusCode = HttpStatusCode.OK.value,
                successMessage = "Icon uploaded successfully"
            ) { it?.toModel()?.toRes() }
    }

    companion object {
        private const val ICON_URL_TTL = 604800L // 7 days
    }
}
