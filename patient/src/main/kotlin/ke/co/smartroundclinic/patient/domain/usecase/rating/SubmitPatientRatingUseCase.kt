package ke.co.smartroundclinic.patient.domain.usecase.rating

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.patient.domain.model.PatientRating
import ke.co.smartroundclinic.patient.domain.model.toEntity
import ke.co.smartroundclinic.patient.domain.model.toModel
import ke.co.smartroundclinic.patient.domain.repository.PatientRatingRepository
import ke.co.smartroundclinic.patient.presentation.dto.response.PatientRatingRes
import ke.co.smartroundclinic.patient.presentation.dto.response.toRes

class SubmitPatientRatingUseCase(private val repository: PatientRatingRepository) {
    suspend operator fun invoke(model: PatientRating): DefaultResponse<PatientRatingRes?> =
        repository.add(model.toEntity()).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
        ) { it?.toModel()?.toRes() }
}
