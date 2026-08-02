package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.usecase.recommendation.GetDoctorByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.recommendation.GetRecommendedDoctorsUseCase

class RecommendationService(
    private val getRecommendedUseCase: GetRecommendedDoctorsUseCase,
    private val getDoctorByIdUseCase: GetDoctorByIdUseCase,
) {
    suspend fun getRecommendations(specializationId: String?, page: Int, size: Int, excludeDoctorId: String? = null) =
        getRecommendedUseCase(specializationId, page, size, excludeDoctorId)

    suspend fun getById(doctorId: String) = getDoctorByIdUseCase(doctorId)
}
