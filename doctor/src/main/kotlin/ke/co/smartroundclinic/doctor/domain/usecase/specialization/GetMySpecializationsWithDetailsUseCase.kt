package ke.co.smartroundclinic.doctor.domain.usecase.specialization

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecializationWithDetailsRes

class GetMySpecializationsWithDetailsUseCase(private val repository: SpecializationRepository) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<List<SpecializationWithDetailsRes>?> =
        repository.getByDoctorIdWithDetails(doctorId)
            .toDefaultResponse { it }
}
