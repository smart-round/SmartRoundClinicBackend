package ke.co.smartroundclinic.support.data.entity

import ke.co.smartroundclinic.support.domain.model.IssueCategory
import org.bson.types.ObjectId
import kotlin.time.Clock

data class IssueCategoryEntity(
    val id: String = ObjectId().toString(),
    val name: String,
    val description: String? = null,
    val status: Boolean = true,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = IssueCategory(
        id = id,
        name = name,
        description = description,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun IssueCategory.toEntity() = IssueCategoryEntity(
    id = id,
    name = name,
    description = description,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
