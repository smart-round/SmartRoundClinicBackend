package ke.co.smartroundclinic.doctor.domain.usecase.specialization

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecializationRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetMySpecializationsUseCase(private val repository: SpecializationRepository) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<List<SpecializationRes>?> =
        repository.getByDoctorId(doctorId).toDefaultResponse { items ->
            items?.map { it.toModel().toRes() }
        }
}