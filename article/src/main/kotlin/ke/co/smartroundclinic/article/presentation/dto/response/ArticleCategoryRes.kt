package ke.co.smartroundclinic.article.presentation.dto.response

import ke.co.smartroundclinic.article.domain.model.ArticleCategory
import kotlinx.serialization.Serializable

@Serializable
data class ArticleCategoryRes(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class ArticleCategoryPageResult(
    val items: List<ArticleCategoryRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun ArticleCategory.toRes() = ArticleCategoryRes(
    id = id,
    name = name,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
