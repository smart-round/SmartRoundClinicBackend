package ke.co.smartroundclinic.article.domain.usecase.article

import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class SuspendArticleByDoctorUseCase(private val repository: ArticleRepository) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<ArticleRes?> =
        repository.suspendByDoctor(id, doctorId).toDefaultResponse { it?.toModel()?.toRes() }
}
