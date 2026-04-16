package ke.co.smartroundclinic.doctor.domain.usecase.certification

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.CertificationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.CertificationRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerLicenceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetCertificationByIdUseCase(
    private val repository: CertificationRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<CertificationRes?> =
        withContext(Dispatchers.IO) {
            val licence = repository.getById(id, doctorId)
            if (licence is Resource.Error) return@withContext licence.toDefaultResponse(
                failedStatusCode = HttpStatusCode.NotFound.value,
                errorMessage = "Certification not found"
            )
            val presignResult = storageRepository.presignedGetUrl(
                bucket = AppConfig.r2.bucket,
                key = licence.data?.certificationUrl ?: "",
                expiresInSeconds = 86400,
            )
            if (presignResult is Resource.Error) return@withContext presignResult.toDefaultResponse(
                failedStatusCode = HttpStatusCode.InternalServerError.value,
                errorMessage = "Failed to generate presigned URL"
            )
            val data = licence.data?.toModel()?.toRes()?.copy(certificationUrl = presignResult.data)

            DefaultResponse(data = data)
        }
}