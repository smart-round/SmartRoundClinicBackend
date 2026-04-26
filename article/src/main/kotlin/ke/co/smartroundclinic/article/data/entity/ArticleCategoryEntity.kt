package ke.co.smartroundclinic.article.data.entity

import ke.co.smartroundclinic.article.domain.model.ArticleCategory
import org.bson.types.ObjectId
import kotlin.time.Clock

data class ArticleCategoryEntity(
    val id: String = ObjectId().toString(),
    val name: String,
    val isActive: Boolean = true,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = ArticleCategory(
        id = id,
        name = name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun ArticleCategory.toEntity() = ArticleCategoryEntity(
    id = id,
    name = name,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
