package ke.co.smartroundclinic.admin.domain.usecase.serviceCategory

import ke.co.smartroundclinic.admin.domain.repository.ServiceCategoryRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateServiceCategoryReq
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceCategoryRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateServiceCategoryUseCase(private val repository: ServiceCategoryRepository) {
    suspend operator fun invoke(id: String, req: UpdateServiceCategoryReq): DefaultResponse<ServiceCategoryRes?> =
        repository.update(id, req.name).toDefaultResponse { it?.toModel()?.toRes() }
}
