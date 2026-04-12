package ke.co.smartroundclinic.admin.domain.usecase

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.response.SpecialityRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(requests: List<CreateSpecialityReq>): DefaultResponse<Nothing?> =
        specialityRepository.createSpeciality(requests.map { it.toEntity() })
            .toDefaultResponse { null }
}
