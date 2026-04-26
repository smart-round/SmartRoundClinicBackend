package ke.co.smartroundclinic.article.presentation.dto.request

import ke.co.smartroundclinic.article.domain.model.ArticleCategory
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class CreateArticleCategoryReq(val name: String) {
    fun toModel() = ArticleCategory(
        id = ObjectId().toString(),
        name = name,
        isActive = true,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}

@Serializable
data class UpdateArticleCategoryReq(val name: String? = null)
