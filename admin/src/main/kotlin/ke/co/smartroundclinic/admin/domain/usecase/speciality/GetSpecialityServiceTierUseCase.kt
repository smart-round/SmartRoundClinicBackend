package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceTierRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource

class GetSpecialityServiceTierUseCase(
    private val specialityRepository: SpecialityRepository,
    private val serviceTierRepository: ServiceTierRepository,
) {
    suspend operator fun invoke(specialityId: String): DefaultResponse<ServiceTierRes?> {
        val specialityResult = specialityRepository.getSpecialityById(specialityId)
        if (specialityResult is Resource.Error) {
            return DefaultResponse(
                httpStatusCode = 404,
                status = false,
                message = specialityResult.message ?: "Speciality not found",
                data = null,
            )
        }
        val serviceTierId = (specialityResult as Resource.Success).data?.serviceTierId
            ?: return DefaultResponse(
                httpStatusCode = 404,
                status = false,
                message = "This speciality has no service tier assigned",
                data = null,
            )
        return serviceTierRepository.getServiceTierById(serviceTierId)
            .toDefaultResponse { it?.toModel()?.toRes() }
    }
}
