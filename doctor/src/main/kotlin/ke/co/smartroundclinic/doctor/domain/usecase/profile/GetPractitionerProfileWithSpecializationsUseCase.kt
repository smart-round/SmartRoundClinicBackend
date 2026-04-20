package ke.co.smartroundclinic.doctor.domain.usecase.profile

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerProfileWithSpecializationsRes

class GetPractitionerProfileWithSpecializationsUseCase(private val repository: PractitionerProfileRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<PractitionerProfileWithSpecializationsRes?> =
        repository.getByIdWithSpecializations(id).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
        ) { it }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Practitioner not found",
                )
            else response
        }
}
