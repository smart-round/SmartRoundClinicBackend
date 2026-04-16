package ke.co.smartroundclinic.doctor.domain.usecase.certification

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.PractitionerCertification
import ke.co.smartroundclinic.doctor.domain.model.toEntity
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.CertificationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.CertificationRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.plugins.UnsupportedFileFormatException
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddCertificationUseCase(
    private val repository: CertificationRepository,
    private val storageRepository: StorageRepository
) {

    suspend operator fun invoke(
        model: PractitionerCertification,
        contentType: String,
        licence: ByteArray
    ): DefaultResponse<CertificationRes?> = withContext(Dispatchers.IO) {

        val extension = imageExtensionOrNull(contentType)
            ?: throw UnsupportedFileFormatException(contentType)

        val key = "certification-licence/${model.id}.$extension"

        val createCertification = repository.add(model.copy(certificationUrl = key).toEntity())
        if (createCertification is Resource.Error) return@withContext createCertification.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value
        )

        val uploadResult = storageRepository.upload(
            bucket = AppConfig.r2.bucket,
            key = key,
            content = licence,
            contentType = contentType,
        )
        if (uploadResult is Resource.Error) return@withContext uploadResult.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Failed to upload certificate "
        ) { null }

        val presignUrl = storageRepository.presignedGetUrl(
            bucket = AppConfig.r2.bucket,
            key =key
        )
        if (presignUrl is Resource.Error) return@withContext presignUrl.toDefaultResponse(
            errorMessage = "Failed to generate presigned URL"
        )
        createCertification.toDefaultResponse{ data->
            data?.copy(certificationUrl = presignUrl.data)?.toModel()?.toRes()
        }
    }
}