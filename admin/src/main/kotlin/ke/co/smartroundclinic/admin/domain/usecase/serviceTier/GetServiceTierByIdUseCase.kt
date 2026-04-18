package ke.co.smartroundclinic.admin.domain.usecase.serviceTier

import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceTierRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetServiceTierByIdUseCase(private val repository: ServiceTierRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<ServiceTierRes?> =
        repository.getServiceTierById(id)
            .toDefaultResponse { it?.toModel()?.toRes() }
}
