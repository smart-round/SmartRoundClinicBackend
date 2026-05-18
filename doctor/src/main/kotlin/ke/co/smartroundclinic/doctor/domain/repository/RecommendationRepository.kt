package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.presentation.dto.response.RecommendedDoctorRes

interface RecommendationRepository {
    suspend fun getRecommendations(
        specializationId: String?,
        page: Int,
        size: Int,
    ): Resource<Pair<List<RecommendedDoctorRes>, Long>>
}
