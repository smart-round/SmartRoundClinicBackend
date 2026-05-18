package ke.co.smartroundclinic.doctor.domain.usecase.recommendation

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.RecommendationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.RecommendedDoctorsPageRes

class GetRecommendedDoctorsUseCase(private val repository: RecommendationRepository) {
    suspend operator fun invoke(
        specializationId: String?,
        page: Int,
        size: Int,
    ): DefaultResponse<RecommendedDoctorsPageRes?> =
        repository.getRecommendations(specializationId, page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                RecommendedDoctorsPageRes(
                    items = items,
                    total = total,
                    page = page,
                    size = size,
                )
            }
        }
}
