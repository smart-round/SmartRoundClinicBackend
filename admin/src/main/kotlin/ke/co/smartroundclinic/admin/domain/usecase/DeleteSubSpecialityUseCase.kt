package ke.co.smartroundclinic.admin.domain.usecase

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse

class DeleteSubSpecialityUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(id: String): DefaultResponse<Nothing?> =
        specialityRepository.deleteSubSpeciality(id)
            .toDefaultResponse { it }
}
