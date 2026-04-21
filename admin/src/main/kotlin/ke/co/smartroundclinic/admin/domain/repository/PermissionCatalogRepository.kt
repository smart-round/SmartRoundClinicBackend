package ke.co.smartroundclinic.admin.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.data.entity.PermissionCatalogEntry

interface PermissionCatalogRepository {
    suspend fun getAll(): Resource<List<PermissionCatalogEntry>>
    suspend fun getByKeys(keys: List<String>): List<PermissionCatalogEntry>
}
