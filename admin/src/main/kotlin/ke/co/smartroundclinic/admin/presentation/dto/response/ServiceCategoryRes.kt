package ke.co.smartroundclinic.admin.presentation.dto.response

import ke.co.smartroundclinic.admin.domain.model.ServiceCategory
import kotlinx.serialization.Serializable

@Serializable
data class ServiceCategoryRes(
    val id: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class ServiceCategoryPageResult(
    val items: List<ServiceCategoryRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun ServiceCategory.toRes() = ServiceCategoryRes(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
