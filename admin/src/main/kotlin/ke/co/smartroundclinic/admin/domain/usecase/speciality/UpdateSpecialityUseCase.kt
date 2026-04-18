package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(
        id: String,
        serviceTierId: String?,
        title: String?,
        description: String?,
        color: String?,
        iconUrl: String?,
    ): DefaultResponse<Nothing?> =
        specialityRepository.updateSpeciality(
            id = id,
            title = title,
            serviceTierId = serviceTierId,
            description = description,
            color = color,
            iconUrl = iconUrl
        ).toDefaultResponse { it }
}
