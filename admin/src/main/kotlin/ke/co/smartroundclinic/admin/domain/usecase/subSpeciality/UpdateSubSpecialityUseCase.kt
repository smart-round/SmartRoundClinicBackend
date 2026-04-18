package ke.co.smartroundclinic.admin.domain.usecase.subSpeciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.SubSpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateSubSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(
        id: String,
        title: String?,
        description: String?,
        color: String?,
        iconUrl: String?,
    ): DefaultResponse<SubSpecialityRes?> =
        specialityRepository.updateSubSpeciality(id, title, description, color, iconUrl)
            .toDefaultResponse { it?.toModel()?.toRes() }
}
