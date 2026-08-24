package ke.co.smartroundclinic.article.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.article.data.entity.ArticleEntity
import ke.co.smartroundclinic.article.data.entity.ArticleReferenceEntity
import ke.co.smartroundclinic.article.data.entity.referencesFromJson
import ke.co.smartroundclinic.article.data.entity.toReferencesJson
import ke.co.smartroundclinic.article.domain.model.ArticleState
import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson
import kotlin.time.Clock

class ArticleRepositoryImpl(database: MongoDatabase) : ArticleRepository {

    private val collection = database.getCollection<ArticleEntity>(MongoDBConstants.ARTICLES)

    override suspend fun create(entity: ArticleEntity): Resource<ArticleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                collection.insertOne(entity)
                Resource.Success(data = entity, message = "Article created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create article")
            }
        }

    override suspend fun getById(id: String): Resource<ArticleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article not found")
                if (entity.state == ArticleState.DELETED.name)
                    return@withContext Resource.Error("Article not found")
                Resource.Success(data = entity, message = "Article retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve article")
            }
        }

    override suspend fun getAll(page: Int, size: Int, liveOnly: Boolean): Resource<Pair<List<ArticleEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val filter: Bson = if (liveOnly)
                    Filters.eq(ArticleEntity::state.name, ArticleState.LIVE.name)
                else
                    Filters.ne(ArticleEntity::state.name, ArticleState.DELETED.name)
                val total = collection.countDocuments(filter)
                val items = collection.find(filter)
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Articles retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve articles")
            }
        }

    override suspend fun getByDoctor(doctorId: String, page: Int, size: Int): Resource<Pair<List<ArticleEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val filter = Filters.and(
                    Filters.eq(ArticleEntity::doctorId.name, doctorId),
                    Filters.ne(ArticleEntity::state.name, ArticleState.DELETED.name),
                )
                val total = collection.countDocuments(filter)
                val items = collection.find(filter)
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Articles retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve doctor articles")
            }
        }

    override suspend fun getByCategory(categoryId: String, page: Int, size: Int): Resource<Pair<List<ArticleEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val filter = Filters.and(
                    Filters.eq(ArticleEntity::categoryId.name, categoryId),
                    Filters.ne(ArticleEntity::state.name, ArticleState.DELETED.name),
                )
                val total = collection.countDocuments(filter)
                val items = collection.find(filter)
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Articles retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve articles by category")
            }
        }

    override suspend fun update(
        id: String,
        title: String?,
        content: String?,
        summary: String?,
        categoryId: String?,
        thumbnailKey: String?,
        references: List<ArticleReferenceEntity>?,
    ): Resource<ArticleEntity?> = withContext(Dispatchers.IO) {
        try {
            val existing = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Article not found")
            if (existing.state == ArticleState.DELETED.name)
                return@withContext Resource.Error("Cannot update a deleted article")

            val updates = mutableListOf<Bson>()
            title?.trim()?.takeIf { it.isNotBlank() && it != existing.title }
                ?.let { updates.add(Updates.set(ArticleEntity::title.name, it)) }
            content?.trim()?.takeIf { it.isNotBlank() && it != existing.content }
                ?.let { updates.add(Updates.set(ArticleEntity::content.name, it)) }
            summary?.trim()?.takeIf { it.isNotBlank() && it != existing.summary }
                ?.let { updates.add(Updates.set(ArticleEntity::summary.name, it)) }
            categoryId?.takeIf { it != existing.categoryId }
                ?.let { updates.add(Updates.set(ArticleEntity::categoryId.name, it)) }
            thumbnailKey?.trim()?.takeIf { it != existing.thumbnailKey }
                ?.let { updates.add(Updates.set(ArticleEntity::thumbnailKey.name, it)) }
            references?.takeIf { it != referencesFromJson(existing.referencesJson) }
                ?.let { updates.add(Updates.set(ArticleEntity::referencesJson.name, it.toReferencesJson())) }

            if (updates.isEmpty()) return@withContext Resource.Success(data = existing, message = "No changes detected")

            updates.add(Updates.set(ArticleEntity::updatedAt.name, Clock.System.now().toString()))
            collection.updateOne(Filters.eq(ArticleEntity::id.name, id), Updates.combine(updates))
            val updated = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
            Resource.Success(data = updated, message = "Article updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update article")
        }
    }

    override suspend fun updateByDoctor(
        id: String,
        doctorId: String,
        title: String?,
        content: String?,
        summary: String?,
        categoryId: String?,
        thumbnailKey: String?,
        references: List<ArticleReferenceEntity>?,
    ): Resource<ArticleEntity?> = withContext(Dispatchers.IO) {
        try {
            val existing = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Article not found")
            if (existing.state == ArticleState.DELETED.name)
                return@withContext Resource.Error("Cannot update a deleted article")
            if (existing.doctorId != doctorId)
                return@withContext Resource.Error("You can only edit your own articles")

            val updates = mutableListOf<Bson>()
            title?.trim()?.takeIf { it.isNotBlank() && it != existing.title }
                ?.let { updates.add(Updates.set(ArticleEntity::title.name, it)) }
            content?.trim()?.takeIf { it.isNotBlank() && it != existing.content }
                ?.let { updates.add(Updates.set(ArticleEntity::content.name, it)) }
            summary?.trim()?.takeIf { it.isNotBlank() && it != existing.summary }
                ?.let { updates.add(Updates.set(ArticleEntity::summary.name, it)) }
            categoryId?.takeIf { it != existing.categoryId }
                ?.let { updates.add(Updates.set(ArticleEntity::categoryId.name, it)) }
            thumbnailKey?.trim()?.takeIf { it != existing.thumbnailKey }
                ?.let { updates.add(Updates.set(ArticleEntity::thumbnailKey.name, it)) }
            references?.takeIf { it != referencesFromJson(existing.referencesJson) }
                ?.let { updates.add(Updates.set(ArticleEntity::referencesJson.name, it.toReferencesJson())) }

            if (updates.isEmpty()) return@withContext Resource.Success(data = existing, message = "No changes detected")

            updates.add(Updates.set(ArticleEntity::updatedAt.name, Clock.System.now().toString()))
            collection.updateOne(Filters.eq(ArticleEntity::id.name, id), Updates.combine(updates))
            val updated = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
            Resource.Success(data = updated, message = "Article updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update article")
        }
    }

    override suspend fun suspendByDoctor(id: String, doctorId: String): Resource<ArticleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article not found")
                if (existing.doctorId != doctorId)
                    return@withContext Resource.Error("You can only unpublish your own articles")
                when (existing.state) {
                    ArticleState.DRAFT.name -> return@withContext Resource.Error("Article is already a draft")
                    ArticleState.DELETED.name -> return@withContext Resource.Error("Cannot unpublish a deleted article")
                }
                collection.updateOne(
                    Filters.eq(ArticleEntity::id.name, id),
                    Updates.combine(
                        Updates.set(ArticleEntity::state.name, ArticleState.DRAFT.name),
                        Updates.set(ArticleEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Article moved back to draft")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to unpublish article")
            }
        }

    override suspend fun deleteByDoctor(id: String, doctorId: String): Resource<ArticleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article not found")
                if (existing.state == ArticleState.DELETED.name)
                    return@withContext Resource.Error("Article is already deleted")
                if (existing.doctorId != doctorId)
                    return@withContext Resource.Error("You can only delete your own articles")
                collection.updateOne(
                    Filters.eq(ArticleEntity::id.name, id),
                    Updates.combine(
                        Updates.set(ArticleEntity::state.name, ArticleState.DELETED.name),
                        Updates.set(ArticleEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Article deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete article")
            }
        }

    override suspend fun publish(id: String): Resource<ArticleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article not found")
                when (existing.state) {
                    ArticleState.LIVE.name -> return@withContext Resource.Error("Article is already live")
                    ArticleState.DELETED.name -> return@withContext Resource.Error("Cannot publish a deleted article")
                }
                if (referencesFromJson(existing.referencesJson).isEmpty())
                    return@withContext Resource.Error("Add at least one medical reference before publishing")
                val now = Clock.System.now().toString()
                val stateUpdates = mutableListOf(
                    Updates.set(ArticleEntity::state.name, ArticleState.LIVE.name),
                    Updates.set(ArticleEntity::updatedAt.name, now),
                )
                // Only set datePosted the first time it goes live
                if (existing.datePosted == null) {
                    stateUpdates.add(Updates.set(ArticleEntity::datePosted.name, now))
                }
                collection.updateOne(Filters.eq(ArticleEntity::id.name, id), Updates.combine(stateUpdates))
                val updated = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Article is now live")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to publish article")
            }
        }

    override suspend fun publishByDoctor(id: String, doctorId: String): Resource<ArticleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article not found")
                if (existing.doctorId != doctorId)
                    return@withContext Resource.Error("You can only publish your own articles")
                when (existing.state) {
                    ArticleState.LIVE.name -> return@withContext Resource.Error("Article is already live")
                    ArticleState.DELETED.name -> return@withContext Resource.Error("Cannot publish a deleted article")
                }
                if (referencesFromJson(existing.referencesJson).isEmpty())
                    return@withContext Resource.Error("Add at least one medical reference before publishing")
                val now = Clock.System.now().toString()
                val updates = mutableListOf(
                    Updates.set(ArticleEntity::state.name, ArticleState.LIVE.name),
                    Updates.set(ArticleEntity::updatedAt.name, now),
                )
                if (existing.datePosted == null) updates.add(Updates.set(ArticleEntity::datePosted.name, now))
                collection.updateOne(Filters.eq(ArticleEntity::id.name, id), Updates.combine(updates))
                val updated = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Article is now live")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to publish article")
            }
        }

    override suspend fun suspendArticle(id: String): Resource<ArticleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article not found")
                when (existing.state) {
                    ArticleState.DRAFT.name -> return@withContext Resource.Error("Cannot suspend a draft article — publish it first")
                    ArticleState.SUSPENDED.name -> return@withContext Resource.Error("Article is already suspended")
                    ArticleState.DELETED.name -> return@withContext Resource.Error("Cannot suspend a deleted article")
                }
                collection.updateOne(
                    Filters.eq(ArticleEntity::id.name, id),
                    Updates.combine(
                        Updates.set(ArticleEntity::state.name, ArticleState.SUSPENDED.name),
                        Updates.set(ArticleEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Article suspended successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to suspend article")
            }
        }

    override suspend fun getDeleted(page: Int, size: Int): Resource<Pair<List<ArticleEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val filter = Filters.eq(ArticleEntity::state.name, ArticleState.DELETED.name)
                val total = collection.countDocuments(filter)
                val items = collection.find(filter)
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Deleted articles retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve deleted articles")
            }
        }

    override suspend fun delete(id: String): Resource<ArticleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Article not found")
                if (existing.state == ArticleState.DELETED.name)
                    return@withContext Resource.Error("Article is already deleted")
                collection.updateOne(
                    Filters.eq(ArticleEntity::id.name, id),
                    Updates.combine(
                        Updates.set(ArticleEntity::state.name, ArticleState.DELETED.name),
                        Updates.set(ArticleEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = collection.find(Filters.eq(ArticleEntity::id.name, id)).firstOrNull()
                Resource.Success(data = updated, message = "Article deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete article")
            }
        }
}
