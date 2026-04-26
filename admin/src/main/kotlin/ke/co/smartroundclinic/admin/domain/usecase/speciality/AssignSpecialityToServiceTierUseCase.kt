package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse

class AssignSpecialityToServiceTierUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(specialityId: String, serviceTierId: String): DefaultResponse<Nothing?> =
        specialityRepository.assignToServiceTier(specialityId, serviceTierId).toDefaultResponse { it }
}
