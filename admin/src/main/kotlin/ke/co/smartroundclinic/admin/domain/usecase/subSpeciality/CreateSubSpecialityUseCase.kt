package ke.co.smartroundclinic.admin.domain.usecase.subSpeciality

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.data.entity.toEntity
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSubSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.response.SubSpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateSubSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        specialityId: String,
        request: CreateSubSpecialityReq,
        imageBytes: ByteArray?,
        contentType: String?,
    ): DefaultResponse<SubSpecialityRes?> = withContext(Dispatchers.IO) {
        val iconUrl = if (imageBytes != null && contentType != null) {
            val model = request.toModel(specialityId)
            val extension = imageExtensionOrNull(contentType) ?: "jpeg"
            val key = "subspeciality-icons/${model.id}.$extension"

            val upload = storageRepository.upload(AppConfig.r2.bucket, key, imageBytes, contentType)
            if (upload is Resource.Error) return@withContext upload.toDefaultResponse(
                failedStatusCode = HttpStatusCode.InternalServerError.value,
                errorMessage = "Failed to upload icon"
            ) { null }

            val presign = storageRepository.presignedGetUrl(AppConfig.r2.bucket, key, ICON_URL_TTL)
            presign.data ?: return@withContext Resource.Error<SubSpecialityRes?>(
                message = "Failed to generate icon URL"
            ).toDefaultResponse(failedStatusCode = HttpStatusCode.InternalServerError.value) { null }
        } else null

        val model = request.toModel(specialityId, iconUrl)
        specialityRepository.createSubSpeciality(specialityId, model.toEntity())
            .toDefaultResponse { it?.toModel()?.toRes() }
    }

    companion object {
        private const val ICON_URL_TTL = 604800L
    }
}
