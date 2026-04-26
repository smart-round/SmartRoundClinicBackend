package ke.co.smartroundclinic.article.domain.usecase.articleCategory

import ke.co.smartroundclinic.article.data.entity.toEntity
import ke.co.smartroundclinic.article.domain.repository.ArticleCategoryRepository
import ke.co.smartroundclinic.article.presentation.dto.request.CreateArticleCategoryReq
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleCategoryRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateArticleCategoryUseCase(private val repository: ArticleCategoryRepository) {
    suspend operator fun invoke(req: CreateArticleCategoryReq): DefaultResponse<ArticleCategoryRes?> =
        repository.create(req.toModel().toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { it?.toModel()?.toRes() }
}
