package ke.co.smartroundclinic.doctor.domain.usecase.rating

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.DoctorRating
import ke.co.smartroundclinic.doctor.domain.model.toEntity
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.RatingRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class SubmitRatingUseCase(private val repository: DoctorRatingRepository) {
    suspend operator fun invoke(model: DoctorRating): DefaultResponse<RatingRes?> =
        repository.add(model.toEntity()).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
        ) { it?.toModel()?.toRes() }
}
