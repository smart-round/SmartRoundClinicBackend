package ke.co.smartroundclinic.admin.domain.usecase.serviceCategory

import ke.co.smartroundclinic.admin.data.entity.toEntity
import ke.co.smartroundclinic.admin.domain.repository.ServiceCategoryRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateServiceCategoryReq
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceCategoryRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateServiceCategoryUseCase(private val repository: ServiceCategoryRepository) {
    suspend operator fun invoke(req: CreateServiceCategoryReq): DefaultResponse<ServiceCategoryRes?> =
        repository.create(req.toModel().toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { it?.toModel()?.toRes() }
}
