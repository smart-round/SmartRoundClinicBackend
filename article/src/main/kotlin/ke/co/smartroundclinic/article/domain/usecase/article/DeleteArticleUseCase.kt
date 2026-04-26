package ke.co.smartroundclinic.article.domain.usecase.article

import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class DeleteArticleUseCase(private val repository: ArticleRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<ArticleRes?> =
        repository.delete(id).toDefaultResponse { it?.toModel()?.toRes() }
}
