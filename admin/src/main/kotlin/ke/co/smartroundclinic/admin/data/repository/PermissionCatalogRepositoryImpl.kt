package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.domain.repository.PermissionCatalogRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.data.entity.PermissionCatalogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class PermissionCatalogRepositoryImpl(
    database: MongoDatabase,
) : PermissionCatalogRepository {

    private val collection = database.getCollection<PermissionCatalogEntry>(MongoDBConstants.ADMIN_PERMISSIONS_CATALOG)

    override suspend fun getAll(): Resource<List<PermissionCatalogEntry>> = withContext(Dispatchers.IO) {
        try {
            val entries = collection.find().toList()
            Resource.Success(data = entries, message = "Permission catalog fetched successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch permission catalog")
        }
    }

    override suspend fun getByKeys(keys: List<String>): List<PermissionCatalogEntry> = withContext(Dispatchers.IO) {
        if (keys.isEmpty()) return@withContext emptyList()
        runCatching {
            collection.find(Filters.`in`(PermissionCatalogEntry::key.name, keys)).toList()
        }.getOrDefault(emptyList())
    }
}
