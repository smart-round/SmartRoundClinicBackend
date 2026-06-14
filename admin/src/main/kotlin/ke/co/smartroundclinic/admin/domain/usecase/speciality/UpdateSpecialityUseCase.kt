package ke.co.smartroundclinic.admin.domain.usecase.speciality

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateSpecialityReq
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        id: String,
        req: UpdateSpecialityReq,
        imageBytes: ByteArray?,
        contentType: String?,
    ): DefaultResponse<Nothing?> = withContext(Dispatchers.IO) {
        val iconKey = if (imageBytes != null && contentType != null) {
            val extension = imageExtensionOrNull(contentType) ?: "jpeg"
            val key = "speciality-icons/$id.$extension"

            val upload = storageRepository.upload(AppConfig.r2.bucket, key, imageBytes, contentType)
            if (upload is Resource.Error) return@withContext upload.toDefaultResponse(
                failedStatusCode = HttpStatusCode.InternalServerError.value,
                errorMessage = "Failed to upload icon"
            ) { null }

            key  // store R2 key, not presigned URL
        } else null

        specialityRepository.updateSpeciality(
            id = id,
            title = req.title,
            serviceTierId = req.serviceTierId,
            description = req.description,
            color = req.color,
            iconUrl = iconKey,
        ).toDefaultResponse { it }
    }
}
