package ke.co.smartroundclinic.admin.data.entity

import ke.co.smartroundclinic.admin.domain.model.PolicyGroup
import org.bson.types.ObjectId

data class PolicyGroupEntity(
    val id: String = ObjectId().toString(),
    val name: String,
    val description: String?,
    val permissions: List<String>,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String? = null,
) {
    fun toModel() = PolicyGroup(
        id = id,
        name = name,
        description = description,
        permissions = permissions,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun PolicyGroup.toEntity() = PolicyGroupEntity(
    id = id,
    name = name,
    description = description,
    permissions = permissions,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
