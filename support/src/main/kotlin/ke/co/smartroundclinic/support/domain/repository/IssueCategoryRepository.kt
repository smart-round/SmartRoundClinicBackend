package ke.co.smartroundclinic.support.domain.repository

import ke.co.smartroundclinic.support.data.entity.IssueCategoryEntity
import ke.co.smartroundclinic.common.Resource

interface IssueCategoryRepository {
    suspend fun create(entity: IssueCategoryEntity): Resource<IssueCategoryEntity?>
    suspend fun getById(id: String): Resource<IssueCategoryEntity?>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<IssueCategoryEntity>, Long>>
    suspend fun update(id: String, name: String?, description: String?, status: Boolean?): Resource<IssueCategoryEntity?>
    suspend fun delete(id: String): Resource<IssueCategoryEntity?>
}
