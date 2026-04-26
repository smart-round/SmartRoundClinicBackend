package ke.co.smartroundclinic.admin.domain.usecase.serviceCategory

import ke.co.smartroundclinic.admin.domain.repository.ServiceCategoryRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.ServiceCategoryRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetServiceCategoryByIdUseCase(private val repository: ServiceCategoryRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<ServiceCategoryRes?> =
        repository.getById(id).toDefaultResponse { it?.toModel()?.toRes() }
}
