package ke.co.smartroundclinic.doctor.domain.usecase.licence

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerLicenceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetLicenceByIdUseCase(
    private val repository: PractitionerLicenceRepository,
    private val storageRepository: StorageRepository,
) {

    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<PractitionerLicenceRes?> =
        withContext(Dispatchers.IO) {
            val licence = repository.getById(id, doctorId)
            if (licence is Resource.Error) return@withContext licence.toDefaultResponse(
                failedStatusCode = HttpStatusCode.NotFound.value,
                errorMessage = "Licence not found"
            )
            val presignResult = storageRepository.presignedGetUrl(
                bucket = AppConfig.r2.bucket,
                key = licence.data?.licenceUrl ?: "",
                expiresInSeconds = 86400,
            )
            if (presignResult is Resource.Error) return@withContext presignResult.toDefaultResponse(
                failedStatusCode = HttpStatusCode.InternalServerError.value,
                errorMessage = "Failed to generate presigned URL"
            )
            val data = licence.data?.toModel()?.toRes()?.copy(licenceUrl = presignResult.data)

            DefaultResponse(data = data)
        }
}