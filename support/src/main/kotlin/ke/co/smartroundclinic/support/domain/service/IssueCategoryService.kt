package ke.co.smartroundclinic.support.domain.service

import ke.co.smartroundclinic.support.domain.usecase.issueCategory.CreateIssueCategoryUseCase
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.DeleteIssueCategoryUseCase
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.GetAllIssueCategoriesUseCase
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.GetIssueCategoryByIdUseCase
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.UpdateIssueCategoryUseCase
import ke.co.smartroundclinic.support.presentation.dto.request.CreateIssueCategoryReq
import ke.co.smartroundclinic.support.presentation.dto.request.UpdateIssueCategoryReq

class IssueCategoryService(
    private val createUseCase: CreateIssueCategoryUseCase,
    private val getByIdUseCase: GetIssueCategoryByIdUseCase,
    private val getAllUseCase: GetAllIssueCategoriesUseCase,
    private val updateUseCase: UpdateIssueCategoryUseCase,
    private val deleteUseCase: DeleteIssueCategoryUseCase,
) {
    suspend fun create(req: CreateIssueCategoryReq) = createUseCase(req)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun update(id: String, req: UpdateIssueCategoryReq) = updateUseCase(id, req)
    suspend fun delete(id: String) = deleteUseCase(id)
}
