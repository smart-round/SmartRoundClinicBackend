package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.data.entity.ServiceTierEntity
import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson
import java.time.Instant

class ServiceTierRepositoryImpl(database: MongoDatabase) : ServiceTierRepository {

    private val collection = database.getCollection<ServiceTierEntity>(MongoDBConstants.ADMIN_SERVICE_TIERS)

    override suspend fun createServiceTier(serviceTierEntity: ServiceTierEntity): Resource<ServiceTierEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(Filters.eq(ServiceTierEntity::name.name, serviceTierEntity.name))
                    .firstOrNull()
                if (existing != null) return@withContext Resource.Error("Service tier with name ${serviceTierEntity.name} already exists")

                collection.insertOne(serviceTierEntity)
                Resource.Success(data = serviceTierEntity, message = "Service tier created successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to create service tier")
            }
        }

    override suspend fun getServiceTierById(id: String): Resource<ServiceTierEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(ServiceTierEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Service tier not found")
                Resource.Success(data = entity, message = "Service tier retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve service tier")
            }
        }

    override suspend fun getAllServiceTiers(page: Int, size: Int): Resource<Pair<List<ServiceTierEntity>, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val safePage = maxOf(1, page)
                val safeSize = minOf(maxOf(1, size), 100)
                val total = collection.countDocuments()
                val items = collection.find()
                    .skip((safePage - 1) * safeSize)
                    .limit(safeSize)
                    .toList()
                Resource.Success(data = items to total, message = "Service tiers retrieved successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve service tiers")
            }
        }

    override suspend fun updateServiceTier(
        id: String,
        name: String?,
        tierPrice: Double?,
        consultationDuration: Long?,
        gracePeriod: Long?,
        chatAccessWindow: Long?,
        followUpWindow: Long?,
        followUpFee: Long?,
    ): Resource<ServiceTierEntity?> = withContext(Dispatchers.IO) {
        try {
            val existing = collection.find(Filters.eq(ServiceTierEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Service tier not found")

            val updates = mutableListOf<Bson>()
            name?.trim()?.takeIf { it.isNotBlank() && it != existing.name }
                ?.let { updates.add(Updates.set(ServiceTierEntity::name.name, it)) }
            tierPrice?.takeIf { it != existing.tierPrice }
                ?.let { updates.add(Updates.set(ServiceTierEntity::tierPrice.name, it)) }
            consultationDuration?.takeIf { it != existing.consultationDuration }
                ?.let { updates.add(Updates.set(ServiceTierEntity::consultationDuration.name, it)) }
            gracePeriod?.takeIf { it != existing.gracePeriod }
                ?.let { updates.add(Updates.set(ServiceTierEntity::gracePeriod.name, it)) }
            chatAccessWindow?.takeIf { it != existing.chatAccessWindow }
                ?.let { updates.add(Updates.set(ServiceTierEntity::chatAccessWindow.name, it)) }
            followUpWindow?.takeIf { it != existing.followUpWindow }
                ?.let { updates.add(Updates.set(ServiceTierEntity::followUpWindow.name, it)) }
            followUpFee?.takeIf { it != existing.followUpFee }
                ?.let { updates.add(Updates.set(ServiceTierEntity::followUpFee.name, it)) }

            if (updates.isEmpty()) return@withContext Resource.Success(data = existing, message = "No changes detected")

            updates.add(Updates.set(ServiceTierEntity::updatedAt.name, Instant.now().toString()))
            collection.updateOne(Filters.eq(ServiceTierEntity::id.name, id), Updates.combine(updates))
            val updated = collection.find(Filters.eq(ServiceTierEntity::id.name, id)).firstOrNull()
            Resource.Success(data = updated, message = "Service tier updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update service tier")
        }
    }

    override suspend fun deleteServiceTier(id: String): Resource<ServiceTierEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = collection.find(Filters.eq(ServiceTierEntity::id.name, id)).firstOrNull()
                    ?: return@withContext Resource.Error("Service tier not found")
                collection.deleteOne(Filters.eq(ServiceTierEntity::id.name, id))
                Resource.Success(data = entity, message = "Service tier deleted successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to delete service tier")
            }
        }
}
