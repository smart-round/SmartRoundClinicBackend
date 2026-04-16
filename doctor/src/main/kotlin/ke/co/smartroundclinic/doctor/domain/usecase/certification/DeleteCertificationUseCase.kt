package ke.co.smartroundclinic.doctor.domain.usecase.certification

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.repository.CertificationRepository
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteCertificationUseCase(
    private val repository: CertificationRepository,
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<Nothing?> = withContext(Dispatchers.IO) {
        val certification = repository.getById(id, doctorId)
        val result = repository.delete(id, doctorId)
        if (result is Resource.Error) return@withContext result.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value
        )
        val deleteCertification = storageRepository.delete(
            bucket = AppConfig.r2.bucket,
            key = certification.data?.certificationUrl
                ?: return@withContext result.toDefaultResponse(
                    failedStatusCode = HttpStatusCode.InternalServerError.value,
                    errorMessage = "Failed to delete certification"
                )
        )
        if (deleteCertification is Resource.Error) return@withContext deleteCertification.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Failed to delete certification"
        )
        result.toDefaultResponse(
            successStatusCode = HttpStatusCode.OK.value,
            successMessage = "Certification deleted successfully"
        )
    }
}