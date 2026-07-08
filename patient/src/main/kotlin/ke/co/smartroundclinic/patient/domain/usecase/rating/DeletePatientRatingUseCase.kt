package ke.co.smartroundclinic.patient.domain.usecase.rating

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.patient.domain.repository.PatientRatingRepository

class DeletePatientRatingUseCase(private val repository: PatientRatingRepository) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<Nothing?> =
        repository.delete(id, doctorId).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            successMessage = "Rating deleted successfully",
        )
}
