package ke.co.smartroundclinic.article.presentation.dto.response

import ke.co.smartroundclinic.article.domain.model.Article
import kotlinx.serialization.Serializable

@Serializable
data class ArticleRes(
    val id: String,
    val doctorId: String,
    val title: String,
    val content: String,
    val summary: String,
    val categoryId: String,
    val thumbnailUrl: String?,
    val state: String,
    val datePosted: String?,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class ArticlePageResult(
    val items: List<ArticleRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

// thumbnailUrl is left as the raw key here; ArticleService replaces it with a presigned URL
fun Article.toRes() = ArticleRes(
    id = id,
    doctorId = doctorId,
    title = title,
    content = content,
    summary = summary,
    categoryId = categoryId,
    thumbnailUrl = thumbnailKey,
    state = state.name,
    datePosted = datePosted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
