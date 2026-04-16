package ke.co.smartroundclinic.doctor.domain.usecase.licence

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteLicenceUseCase(
    private val repository: PractitionerLicenceRepository,
    private val storageRepository: StorageRepository
    ) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<Boolean?> = withContext(Dispatchers.IO) {
        val licence = repository.getById(id, doctorId)
        if (licence is Resource.Error) return@withContext licence.toDefaultResponse()
        val result = repository.delete(id, doctorId)
        if (result is Resource.Error) return@withContext result.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value
        )
        val deleteCertification = storageRepository.delete(
            bucket = AppConfig.r2.bucket,
            key = licence.data?.licenceUrl
                ?: return@withContext result.toDefaultResponse(
                    failedStatusCode = HttpStatusCode.InternalServerError.value,
                    errorMessage = "Failed to delete licence"
                )
        )
        if (deleteCertification is Resource.Error) return@withContext deleteCertification.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            errorMessage = "Failed to delete licence"
        )
        result.toDefaultResponse(
            successStatusCode = HttpStatusCode.OK.value,
            successMessage = "Licence deleted successfully"
        )
    }

}