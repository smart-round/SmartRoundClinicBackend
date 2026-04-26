package ke.co.smartroundclinic.article.domain.usecase.articleCategory

import ke.co.smartroundclinic.article.domain.repository.ArticleCategoryRepository
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleCategoryPageResult
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import kotlin.math.ceil

class GetAllArticleCategoriesUseCase(private val repository: ArticleCategoryRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<ArticleCategoryPageResult?> =
        repository.getAll(page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                ArticleCategoryPageResult(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
