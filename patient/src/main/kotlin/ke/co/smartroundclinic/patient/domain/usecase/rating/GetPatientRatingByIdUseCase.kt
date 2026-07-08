package ke.co.smartroundclinic.patient.domain.usecase.rating

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.patient.domain.model.toModel
import ke.co.smartroundclinic.patient.domain.repository.PatientRatingRepository
import ke.co.smartroundclinic.patient.presentation.dto.response.PatientRatingRes
import ke.co.smartroundclinic.patient.presentation.dto.response.toRes

class GetPatientRatingByIdUseCase(private val repository: PatientRatingRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<PatientRatingRes?> =
        repository.getById(id).toDefaultResponse { entity ->
            entity?.toModel()?.toRes()
        }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Rating not found",
                )
            else response
        }
}
