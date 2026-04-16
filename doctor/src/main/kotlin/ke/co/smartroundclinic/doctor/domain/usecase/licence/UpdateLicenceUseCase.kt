package ke.co.smartroundclinic.doctor.domain.usecase.licence

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerLicenceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class UpdateLicenceUseCase(private val repository: PractitionerLicenceRepository) {
    suspend operator fun invoke(
        id: String,
        doctorId: String,
        licenceName: String?,
    ): DefaultResponse<PractitionerLicenceRes?> =
        repository.update(id, doctorId, licenceName).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            successMessage = "Licence updated successfully",
        ) { it?.toModel()?.toRes() }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Licence not found",
                )
            else response
        }
}
