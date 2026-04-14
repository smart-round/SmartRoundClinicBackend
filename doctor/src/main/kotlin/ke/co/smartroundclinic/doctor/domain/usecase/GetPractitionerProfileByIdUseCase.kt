package ke.co.smartroundclinic.doctor.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.PractitionerProfileRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetPractitionerProfileByIdUseCase(private val repository: PractitionerProfileRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<PractitionerProfileRes?> =
        repository.getById(id).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
        ) { it?.toModel()?.toRes() }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Practitioner not found",
                )
            else response
        }
}
