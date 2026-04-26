package ke.co.smartroundclinic.article.domain.service

import ke.co.smartroundclinic.article.domain.usecase.articleCategory.CreateArticleCategoryUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.DeleteArticleCategoryUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.GetAllArticleCategoriesUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.GetArticleCategoryByIdUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.ToggleArticleCategoryUseCase
import ke.co.smartroundclinic.article.domain.usecase.articleCategory.UpdateArticleCategoryUseCase
import ke.co.smartroundclinic.article.presentation.dto.request.CreateArticleCategoryReq
import ke.co.smartroundclinic.article.presentation.dto.request.UpdateArticleCategoryReq

class ArticleCategoryService(
    private val createUseCase: CreateArticleCategoryUseCase,
    private val getByIdUseCase: GetArticleCategoryByIdUseCase,
    private val getAllUseCase: GetAllArticleCategoriesUseCase,
    private val updateUseCase: UpdateArticleCategoryUseCase,
    private val toggleUseCase: ToggleArticleCategoryUseCase,
    private val deleteUseCase: DeleteArticleCategoryUseCase,
) {
    suspend fun create(req: CreateArticleCategoryReq) = createUseCase(req)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun update(id: String, req: UpdateArticleCategoryReq) = updateUseCase(id, req)
    suspend fun toggle(id: String) = toggleUseCase(id)
    suspend fun delete(id: String) = deleteUseCase(id)
}
