package ke.co.smartroundclinic.admin.domain.usecase

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(
        id: String,
        title: String?,
        description: String?,
        color: String?,
        iconUrl: String?,
    ): DefaultResponse<Nothing?> =
        specialityRepository.updateSpeciality(id, title, description, color, iconUrl)
            .toDefaultResponse { it }
}
