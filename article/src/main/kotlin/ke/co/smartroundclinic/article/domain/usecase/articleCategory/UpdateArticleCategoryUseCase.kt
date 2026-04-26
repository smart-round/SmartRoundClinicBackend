package ke.co.smartroundclinic.article.domain.usecase.articleCategory

import ke.co.smartroundclinic.article.domain.repository.ArticleCategoryRepository
import ke.co.smartroundclinic.article.presentation.dto.request.UpdateArticleCategoryReq
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleCategoryRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateArticleCategoryUseCase(private val repository: ArticleCategoryRepository) {
    suspend operator fun invoke(id: String, req: UpdateArticleCategoryReq): DefaultResponse<ArticleCategoryRes?> =
        repository.update(id, req.name).toDefaultResponse { it?.toModel()?.toRes() }
}
