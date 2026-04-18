package ke.co.smartroundclinic.admin.domain.usecase.serviceTier

import ke.co.smartroundclinic.admin.data.entity.toEntity
import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateServiceTierReq
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceTierRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateServiceTierUseCase(private val repository: ServiceTierRepository) {
    suspend operator fun invoke(req: CreateServiceTierReq): DefaultResponse<ServiceTierRes?> =
        repository.createServiceTier(req.toModel().toEntity())
            .toDefaultResponse(
                successStatusCode = 201,
                failedStatusCode = 400,
            ) { it?.toModel()?.toRes() }
}
