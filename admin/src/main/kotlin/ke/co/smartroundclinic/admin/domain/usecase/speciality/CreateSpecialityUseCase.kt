package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.data.entity.toEntity
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSpecialityReq
import ke.co.smartroundclinic.common.DefaultResponse

class CreateSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(requests: List<CreateSpecialityReq>): DefaultResponse<Nothing?> =
        specialityRepository.createSpeciality(requests.map { it.toModel().toEntity() })
            .toDefaultResponse { null }
}
