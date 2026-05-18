package ke.co.smartroundclinic.doctor.domain.usecase.rating

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.RatingsPageRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetDoctorRatingsUseCase(private val repository: DoctorRatingRepository) {
    suspend operator fun invoke(
        doctorId: String,
        page: Int,
        size: Int,
    ): DefaultResponse<RatingsPageRes?> =
        repository.getByDoctorId(doctorId, page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                RatingsPageRes(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                )
            }
        }
}
