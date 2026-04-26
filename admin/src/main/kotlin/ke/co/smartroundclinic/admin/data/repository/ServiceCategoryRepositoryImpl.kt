package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.data.entity.ServiceCategoryEntity
import ke.co.smartroundclinic.admin.domain.repository.ServiceCategoryRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class ServiceCategoryRepositoryImpl(database: MongoDatabase) : ServiceCategoryRepository {

    private val collection = database.getCollection<ServiceCategoryEntity>(MongoDBConstants.ADMIN_SERVICE_CATEGORIES)

    override suspend fun create(entity: ServiceCategoryEntity): Resource<ServiceCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection
                    .find(Filters.regex(ServiceCategoryEntity::name.name, "^${entity.name}$", "i"))
                    .firstOrNull()
                if (existing != null) return@withContext Resource.Error("Service category '${entity.name}' already exists")
                collection.insertOne(entity)
                Resource.Success(data = entity, message = "Service category created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create service category")
            }
        }

    override suspend fun getById(id: String): Resource<ServiceCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(ServiceCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Service category not found")
                Resource.Success(data = entity, message = "Service category retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve service category")
            }
        }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<ServiceCategoryEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val total = collection.countDocuments()
                val items = collection.find()
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Service categories retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve service categories")
            }
        }

    override suspend fun update(id: String, name: String?): Resource<ServiceCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ServiceCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Service category not found")

                val trimmed = name?.trim()?.takeIf { it.isNotBlank() && it != existing.name }
                    ?: return@withContext Resource.Success(data = existing, message = "No changes detected")

                collection.updateOne(
                    Filters.eq(ServiceCategoryEntity::id.name, id),
                    Updates.combine(
                        Updates.set(ServiceCategoryEntity::name.name, trimmed),
                        Updates.set(ServiceCategoryEntity::updatedAtString.name, Clock.System.now().toString()),
                    )
                )
                val updated = collection.find(Filters.eq(ServiceCategoryEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Service category updated successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to update service category")
            }
        }

    override suspend fun delete(id: String): Resource<ServiceCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(ServiceCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Service category not found")
                collection.deleteOne(Filters.eq(ServiceCategoryEntity::id.name, id))
                Resource.Success(data = entity, message = "Service category deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete service category")
            }
        }
}
