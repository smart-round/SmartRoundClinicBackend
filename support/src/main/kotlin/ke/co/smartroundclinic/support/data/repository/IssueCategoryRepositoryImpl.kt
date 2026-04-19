package ke.co.smartroundclinic.support.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.support.data.entity.IssueCategoryEntity
import ke.co.smartroundclinic.support.domain.repository.IssueCategoryRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson
import java.time.Instant

class IssueCategoryRepositoryImpl(database: MongoDatabase) : IssueCategoryRepository {

    private val collection = database.getCollection<IssueCategoryEntity>(MongoDBConstants.SUPPORT_ISSUE_CATEGORIES)

    override suspend fun create(entity: IssueCategoryEntity): Resource<IssueCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(
                    Filters.regex(IssueCategoryEntity::name.name, "^${entity.name}$", "i")
                ).firstOrNull()
                if (existing != null) return@withContext Resource.Error("Issue category '${entity.name}' already exists")
                collection.insertOne(entity)
                Resource.Success(data = entity, message = "Issue category created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create issue category")
            }
        }

    override suspend fun getById(id: String): Resource<IssueCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(IssueCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Issue category not found")
                Resource.Success(data = entity, message = "Issue category retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve issue category")
            }
        }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<IssueCategoryEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val total = collection.countDocuments()
                val items = collection.find()
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Issue categories retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve issue categories")
            }
        }

    override suspend fun update(
        id: String,
        name: String?,
        description: String?,
        status: Boolean?,
    ): Resource<IssueCategoryEntity?> = withContext(Dispatchers.IO) {
        try {
            val existing = collection.find(Filters.eq(IssueCategoryEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Issue category not found")

            val updates = mutableListOf<Bson>()

            name?.trim()?.takeIf { it.isNotBlank() && it != existing.name }?.let { newName ->
                val duplicate = collection.find(
                    Filters.and(
                        Filters.regex(IssueCategoryEntity::name.name, "^$newName$", "i"),
                        Filters.ne(IssueCategoryEntity::id.name, id)
                    )
                ).firstOrNull()
                if (duplicate != null) return@withContext Resource.Error("Issue category '$newName' already exists")
                updates.add(Updates.set(IssueCategoryEntity::name.name, newName))
            }
            description?.trim()?.takeIf { it != existing.description }
                ?.let { updates.add(Updates.set(IssueCategoryEntity::description.name, it)) }
            status?.takeIf { it != existing.status }
                ?.let { updates.add(Updates.set(IssueCategoryEntity::status.name, it)) }

            if (updates.isEmpty()) return@withContext Resource.Success(data = existing, message = "No changes detected")

            updates.add(Updates.set(IssueCategoryEntity::updatedAt.name, Instant.now().toString()))
            collection.updateOne(Filters.eq(IssueCategoryEntity::id.name, id), Updates.combine(updates))
            val updated = collection.find(Filters.eq(IssueCategoryEntity::id.name, id)).firstOrNull()
            Resource.Success(data = updated, message = "Issue category updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update issue category")
        }
    }

    override suspend fun delete(id: String): Resource<IssueCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(IssueCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Issue category not found")
                collection.deleteOne(Filters.eq(IssueCategoryEntity::id.name, id))
                Resource.Success(data = entity, message = "Issue category deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete issue category")
            }
        }
}
