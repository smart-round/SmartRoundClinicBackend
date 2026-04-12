package ke.co.smartroundclinic.admin.domain.usecase

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.SpecialityRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetSpecialitiesUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(): DefaultResponse<List<SpecialityRes>?> =
        specialityRepository.getSpecialities()
            .toDefaultResponse { list -> list?.map { it.toModel().toRes()  } }
}
