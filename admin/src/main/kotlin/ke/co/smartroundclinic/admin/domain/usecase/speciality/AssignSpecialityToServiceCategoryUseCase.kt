package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse

class AssignSpecialityToServiceCategoryUseCase(
    private val specialityRepository: SpecialityRepository,
) {
    suspend operator fun invoke(specialityId: String, serviceCategoryId: String): DefaultResponse<Nothing?> =
        specialityRepository.assignToServiceCategory(specialityId, serviceCategoryId).toDefaultResponse { it }
}
