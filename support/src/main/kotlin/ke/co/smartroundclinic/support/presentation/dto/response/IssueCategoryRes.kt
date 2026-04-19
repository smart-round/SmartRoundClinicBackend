package ke.co.smartroundclinic.support.presentation.dto.response

import ke.co.smartroundclinic.support.domain.model.IssueCategory
import kotlinx.serialization.Serializable

@Serializable
data class IssueCategoryRes(
    val id: String,
    val name: String,
    val description: String?,
    val status: Boolean,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class IssueCategoryPageResult(
    val items: List<IssueCategoryRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun IssueCategory.toRes() = IssueCategoryRes(
    id = id,
    name = name,
    description = description,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
