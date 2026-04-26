package ke.co.smartroundclinic.article.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.article.data.entity.ArticleCategoryEntity
import ke.co.smartroundclinic.article.domain.repository.ArticleCategoryRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class ArticleCategoryRepositoryImpl(database: MongoDatabase) : ArticleCategoryRepository {

    private val collection = database.getCollection<ArticleCategoryEntity>(MongoDBConstants.ARTICLE_CATEGORIES)

    override suspend fun create(entity: ArticleCategoryEntity): Resource<ArticleCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection
                    .find(Filters.regex(ArticleCategoryEntity::name.name, "^${entity.name}$", "i"))
                    .firstOrNull()
                if (existing != null) return@withContext Resource.Error("Article category '${entity.name}' already exists")
                collection.insertOne(entity)
                Resource.Success(data = entity, message = "Article category created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create article category")
            }
        }

    override suspend fun getById(id: String): Resource<ArticleCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(ArticleCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article category not found")
                Resource.Success(data = entity, message = "Article category retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve article category")
            }
        }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<ArticleCategoryEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val total = collection.countDocuments()
                val items = collection.find()
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Article categories retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve article categories")
            }
        }

    override suspend fun update(id: String, name: String?): Resource<ArticleCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ArticleCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article category not found")
                val trimmed = name?.trim()?.takeIf { it.isNotBlank() && it != existing.name }
                    ?: return@withContext Resource.Success(data = existing, message = "No changes detected")
                collection.updateOne(
                    Filters.eq(ArticleCategoryEntity::id.name, id),
                    Updates.combine(
                        Updates.set(ArticleCategoryEntity::name.name, trimmed),
                        Updates.set(ArticleCategoryEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = collection.find(Filters.eq(ArticleCategoryEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Article category updated successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to update article category")
            }
        }

    override suspend fun toggleActive(id: String): Resource<ArticleCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ArticleCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article category not found")
                collection.updateOne(
                    Filters.eq(ArticleCategoryEntity::id.name, id),
                    Updates.combine(
                        Updates.set(ArticleCategoryEntity::isActive.name, !existing.isActive),
                        Updates.set(ArticleCategoryEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = collection.find(Filters.eq(ArticleCategoryEntity::id.name, id)).firstOrNull()
                val state = if (updated?.isActive == true) "activated" else "deactivated"
                Resource.Success(data = updated, message = "Article category $state successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to toggle article category")
            }
        }

    override suspend fun delete(id: String): Resource<ArticleCategoryEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(ArticleCategoryEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article category not found")
                collection.deleteOne(Filters.eq(ArticleCategoryEntity::id.name, id))
                Resource.Success(data = entity, message = "Article category deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete article category")
            }
        }
}
