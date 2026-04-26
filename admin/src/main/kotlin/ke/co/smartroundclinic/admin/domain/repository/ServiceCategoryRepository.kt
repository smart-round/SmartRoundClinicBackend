package ke.co.smartroundclinic.admin.domain.repository

import ke.co.smartroundclinic.admin.data.entity.ServiceCategoryEntity
import ke.co.smartroundclinic.common.Resource

interface ServiceCategoryRepository {
    suspend fun create(entity: ServiceCategoryEntity): Resource<ServiceCategoryEntity?>
    suspend fun getById(id: String): Resource<ServiceCategoryEntity?>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<ServiceCategoryEntity>, Long>>
    suspend fun update(id: String, name: String?): Resource<ServiceCategoryEntity?>
    suspend fun delete(id: String): Resource<ServiceCategoryEntity?>
}
