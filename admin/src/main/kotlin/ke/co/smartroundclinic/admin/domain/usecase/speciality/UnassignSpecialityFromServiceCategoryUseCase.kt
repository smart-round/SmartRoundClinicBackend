package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse

class UnassignSpecialityFromServiceCategoryUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(specialityId: String): DefaultResponse<Nothing?> =
        specialityRepository.unassignFromServiceCategory(specialityId).toDefaultResponse { it }
}
