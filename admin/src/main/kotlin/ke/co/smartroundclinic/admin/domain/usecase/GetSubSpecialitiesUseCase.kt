package ke.co.smartroundclinic.admin.domain.usecase

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.SubSpecialityRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetSubSpecialitiesUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(specialityId: String): DefaultResponse<List<SubSpecialityRes>?> =
        specialityRepository.getSubSpecialities(specialityId)
            .toDefaultResponse { list -> list?.map { it.toModel().toRes() } }
}
