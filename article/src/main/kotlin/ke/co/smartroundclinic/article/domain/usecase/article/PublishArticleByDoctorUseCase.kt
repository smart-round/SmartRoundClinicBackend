package ke.co.smartroundclinic.article.domain.usecase.article

import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class PublishArticleByDoctorUseCase(private val repository: ArticleRepository) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<ArticleRes?> =
        repository.publishByDoctor(id, doctorId).toDefaultResponse { it?.toModel()?.toRes() }
}
