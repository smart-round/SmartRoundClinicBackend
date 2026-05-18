package ke.co.smartroundclinic.doctor.domain.usecase.rating

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository

class DeleteRatingUseCase(private val repository: DoctorRatingRepository) {
    suspend operator fun invoke(id: String, patientId: String): DefaultResponse<Nothing?> =
        repository.delete(id, patientId).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            successMessage = "Rating deleted successfully",
        )
}
