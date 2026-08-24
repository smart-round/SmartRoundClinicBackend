package ke.co.smartroundclinic.article.presentation.dto.request

import ke.co.smartroundclinic.article.domain.model.Article
import ke.co.smartroundclinic.article.domain.model.ArticleState
import ke.co.smartroundclinic.article.presentation.dto.ArticleReferenceDto
import ke.co.smartroundclinic.article.presentation.dto.toModel
import org.bson.types.ObjectId
import kotlin.time.Clock

data class CreateArticleReq(
    val title: String,
    val content: String,
    val summary: String,
    val categoryId: String,
    val thumbnailUrl: String? = null,
    val references: List<ArticleReferenceDto> = emptyList(),
) {
    fun toModel(doctorId: String) = Article(
        id = ObjectId().toString(),
        doctorId = doctorId,
        title = title,
        content = content,
        summary = summary,
        categoryId = categoryId,
        thumbnailKey = thumbnailUrl,
        state = ArticleState.DRAFT,
        datePosted = null,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
        references = references.map { it.toModel() },
    )
}

data class UpdateArticleReq(
    val title: String? = null,
    val content: String? = null,
    val summary: String? = null,
    val categoryId: String? = null,
    val thumbnailUrl: String? = null,
    // null = leave references untouched; a (possibly empty) list replaces them entirely
    val references: List<ArticleReferenceDto>? = null,
)
