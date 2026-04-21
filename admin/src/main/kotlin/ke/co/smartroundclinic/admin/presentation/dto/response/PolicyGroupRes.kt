package ke.co.smartroundclinic.admin.presentation.dto.response

import ke.co.smartroundclinic.admin.domain.model.PolicyGroup
import ke.co.smartroundclinic.infra.data.entity.PermissionCatalogEntry

data class PolicyGroupRes(
    val id: String,
    val name: String,
    val description: String?,
    val permissions: List<String>,
    val permissionCatalogs: List<PermissionCatalogEntry>,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String?,
)

fun PolicyGroup.toRes(catalogs: List<PermissionCatalogEntry> = emptyList()) = PolicyGroupRes(
    id = id,
    name = name,
    description = description,
    permissions = permissions,
    permissionCatalogs = catalogs,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
