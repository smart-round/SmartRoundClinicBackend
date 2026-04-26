package ke.co.smartroundclinic.article.domain.usecase.article

import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.article.presentation.dto.response.ArticlePageResult
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import kotlin.math.ceil

class GetAllArticlesUseCase(private val repository: ArticleRepository) {
    suspend operator fun invoke(page: Int, size: Int, liveOnly: Boolean): DefaultResponse<ArticlePageResult?> =
        repository.getAll(page, size, liveOnly).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                ArticlePageResult(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
