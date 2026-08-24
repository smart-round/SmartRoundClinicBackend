package ke.co.smartroundclinic.article.data.entity

import ke.co.smartroundclinic.article.domain.model.Article
import ke.co.smartroundclinic.article.domain.model.ArticleState
import org.bson.types.ObjectId
import kotlin.time.Clock

data class ArticleEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val title: String,
    val content: String,
    val summary: String,
    val categoryId: String,
    val thumbnailKey: String? = null,
    val state: String = ArticleState.DRAFT.name,
    val datePosted: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
    val references: List<ArticleReferenceEntity> = emptyList(),
) {
    fun toModel() = Article(
        id = id,
        doctorId = doctorId,
        title = title,
        content = content,
        summary = summary,
        categoryId = categoryId,
        thumbnailKey = thumbnailKey,
        state = runCatching { ArticleState.valueOf(state) }.getOrDefault(ArticleState.DRAFT),
        datePosted = datePosted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        references = references.map { it.toModel() },
    )
}

fun Article.toEntity() = ArticleEntity(
    id = id,
    doctorId = doctorId,
    title = title,
    content = content,
    summary = summary,
    categoryId = categoryId,
    thumbnailKey = thumbnailKey,
    state = state.name,
    datePosted = datePosted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    references = references.map { it.toEntity() },
)
