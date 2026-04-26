package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.usecase.serviceCategory.CreateServiceCategoryUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceCategory.DeleteServiceCategoryUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceCategory.GetAllServiceCategoriesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceCategory.GetServiceCategoryByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceCategory.UpdateServiceCategoryUseCase
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateServiceCategoryReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateServiceCategoryReq

class ServiceCategoryService(
    private val createUseCase: CreateServiceCategoryUseCase,
    private val getByIdUseCase: GetServiceCategoryByIdUseCase,
    private val getAllUseCase: GetAllServiceCategoriesUseCase,
    private val updateUseCase: UpdateServiceCategoryUseCase,
    private val deleteUseCase: DeleteServiceCategoryUseCase,
) {
    suspend fun create(req: CreateServiceCategoryReq) = createUseCase(req)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun update(id: String, req: UpdateServiceCategoryReq) = updateUseCase(id, req)
    suspend fun delete(id: String) = deleteUseCase(id)
}
