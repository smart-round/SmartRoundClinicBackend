package ke.co.smartroundclinic.admin.domain.usecase.speciality

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.data.entity.toEntity
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.domain.usecase.resolveIconUrl
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.response.SpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        req: CreateSpecialityReq,
        imageBytes: ByteArray?,
        contentType: String?,
    ): DefaultResponse<SpecialityRes?> = withContext(Dispatchers.IO) {
        val iconKey = if (imageBytes != null && contentType != null) {
            val model = req.toModel()
            val extension = imageExtensionOrNull(contentType) ?: "jpeg"
            val key = "speciality-icons/${model.id}.$extension"

            val upload = storageRepository.upload(AppConfig.r2.bucket, key, imageBytes, contentType)
            if (upload is Resource.Error) return@withContext upload.toDefaultResponse(
                failedStatusCode = HttpStatusCode.InternalServerError.value,
                errorMessage = "Failed to upload icon"
            ) { null }

            key  // store R2 key, not presigned URL
        } else null

        val model = req.toModel(iconKey)
        val responseIconUrl = resolveIconUrl(iconKey, storageRepository)
        specialityRepository.createSpeciality(listOf(model.toEntity()))
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { model.copy(iconUrl = responseIconUrl).toRes() }
    }
}
