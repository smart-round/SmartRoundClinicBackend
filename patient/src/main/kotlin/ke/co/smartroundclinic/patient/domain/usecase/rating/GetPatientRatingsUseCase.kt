package ke.co.smartroundclinic.patient.domain.usecase.rating

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.patient.domain.model.toModel
import ke.co.smartroundclinic.patient.domain.repository.PatientRatingRepository
import ke.co.smartroundclinic.patient.presentation.dto.response.PatientRatingsPageRes
import ke.co.smartroundclinic.patient.presentation.dto.response.toRes

class GetPatientRatingsUseCase(private val repository: PatientRatingRepository) {
    suspend operator fun invoke(
        patientId: String,
        page: Int,
        size: Int,
    ): DefaultResponse<PatientRatingsPageRes?> =
        repository.getByPatientId(patientId, page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                PatientRatingsPageRes(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                )
            }
        }
}
