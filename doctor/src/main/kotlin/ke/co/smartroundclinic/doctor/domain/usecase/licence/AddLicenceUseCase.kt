package ke.co.smartroundclinic.doctor.domain.usecase.licence

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.PractitionerLicence
import ke.co.smartroundclinic.doctor.domain.model.toEntity
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetMyProfileUseCase
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerLicenceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.plugins.UnsupportedFileFormatException
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddLicenceUseCase(
    private val repository: PractitionerLicenceRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        model: PractitionerLicence,
        contentType: String,
        licence: ByteArray
    ): DefaultResponse<PractitionerLicenceRes?> = withContext(Dispatchers.IO) {

        val extension = imageExtensionOrNull(contentType)
            ?: throw UnsupportedFileFormatException(contentType)

        val key = "practitioner-licence/${model.id}.$extension"

        val createLicence = repository.add(model.copy(licenceUrl = key).toEntity())
        if (createLicence is Resource.Error) return@withContext createLicence.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Failed to create licence "
        )

        val uploadResult = storageRepository.upload(
            bucket = AppConfig.r2.bucket,
            key = key,
            content = licence,
            contentType = contentType,
        )
        if (uploadResult is Resource.Error) return@withContext uploadResult.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Failed to upload licence "
        ) { null }

        val presignUrl = storageRepository.presignedGetUrl(
            bucket = AppConfig.r2.bucket,
            key =key
        )
        if (presignUrl is Resource.Error) return@withContext presignUrl.toDefaultResponse(
            errorMessage = "Failed to generate presigned URL"
        )
        createLicence.toDefaultResponse{data->
            data?.copy(licenceUrl = presignUrl.data)?.toModel()?.toRes()
        }


    }
}