package ke.co.smartroundclinic.article.domain.usecase.articleCategory

import ke.co.smartroundclinic.article.domain.repository.ArticleCategoryRepository
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleCategoryRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class DeleteArticleCategoryUseCase(private val repository: ArticleCategoryRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<ArticleCategoryRes?> =
        repository.delete(id).toDefaultResponse { it?.toModel()?.toRes() }
}
