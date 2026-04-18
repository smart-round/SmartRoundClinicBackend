package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse

class DeleteSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(id: String): DefaultResponse<Nothing?> =
        specialityRepository.deleteSpeciality(id)
            .toDefaultResponse { it }
}
