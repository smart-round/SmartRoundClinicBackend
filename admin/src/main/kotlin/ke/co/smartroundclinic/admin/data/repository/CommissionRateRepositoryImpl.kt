package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.data.entity.CommissionRateEntity
import ke.co.smartroundclinic.admin.domain.repository.CommissionRateRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import java.time.Instant

class CommissionRateRepositoryImpl(database: MongoDatabase) : CommissionRateRepository {

    private val collection = database.getCollection<CommissionRateEntity>(MongoDBConstants.ADMIN_COMMISSION_RATES)

    override suspend fun create(entity: CommissionRateEntity): Resource<CommissionRateEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val exists = collection.find().firstOrNull()
                if (exists != null) return@withContext Resource.Error("Commission rate already exists")
                collection.insertOne(entity)
                Resource.Success(data = entity, message = "Commission rate created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create commission rate")
            }
        }

    override suspend fun getById(): Resource<CommissionRateEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find().firstOrNull()
                    ?: return@withContext Resource.Error("Commission rate not found")
                Resource.Success(data = entity, message = "Commission rate retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve commission rate")
            }
        }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<CommissionRateEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val total = collection.countDocuments()
                val items = collection.find()
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Commission rates retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve commission rates")
            }
        }

    override suspend fun upsert(commissionRate: Double, adminId: String): Resource<CommissionRateEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find().firstOrNull()
                if (existing == null) {
                    val entity = CommissionRateEntity(adminId = adminId, commissionRate = commissionRate)
                    collection.insertOne(entity)
                    return@withContext Resource.Success(data = entity, message = "Commission rate created successfully")
                }

                if (commissionRate == existing.commissionRate) {
                    return@withContext Resource.Success(data = existing, message = "No changes detected")
                }

                collection.updateOne(
                    Filters.eq(CommissionRateEntity::id.name, existing.id),
                    Updates.combine(
                        Updates.set(CommissionRateEntity::commissionRate.name, commissionRate),
                        Updates.set(CommissionRateEntity::updatedAt.name, Instant.now().toString()),
                    ),
                )
                val updated = collection.find(Filters.eq(CommissionRateEntity::id.name, existing.id)).firstOrNull()
                Resource.Success(data = updated, message = "Commission rate updated successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to save commission rate")
            }
        }

    override suspend fun delete(id: String): Resource<CommissionRateEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(CommissionRateEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Commission rate not found")
                collection.deleteOne(Filters.eq(CommissionRateEntity::id.name, id))
                Resource.Success(data = entity, message = "Commission rate deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete commission rate")
            }
        }
}
