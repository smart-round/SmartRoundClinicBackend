package ke.co.smartroundclinic.doctor.domain.usecase.rating

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.RatingRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class UpdateRatingUseCase(private val repository: DoctorRatingRepository) {
    suspend operator fun invoke(
        id: String,
        patientId: String,
        rating: Int?,
        comment: String?,
    ): DefaultResponse<RatingRes?> =
        repository.update(id, patientId, rating, comment).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            successMessage = "Rating updated successfully",
        ) { it?.toModel()?.toRes() }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Rating not found",
                )
            else response
        }
}
