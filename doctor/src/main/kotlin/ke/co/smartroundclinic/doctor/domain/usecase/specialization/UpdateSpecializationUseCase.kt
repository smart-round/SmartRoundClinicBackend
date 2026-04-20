package ke.co.smartroundclinic.doctor.domain.usecase.specialization

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecializationRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class UpdateSpecializationUseCase(private val repository: SpecializationRepository) {
    suspend operator fun invoke(
        id: String,
        doctorId: String,
        specializationId: String,
        subSpecializationId: String?,
    ): DefaultResponse<SpecializationRes?> =
        repository.update(id, doctorId, specializationId, subSpecializationId)
            .toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
