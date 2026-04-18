package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.SpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetSpecialityByIdUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(id: String): DefaultResponse<SpecialityRes?> =
        specialityRepository.getSpecialityById(id)
            .toDefaultResponse { it?.toModel()?.toRes() }
}
