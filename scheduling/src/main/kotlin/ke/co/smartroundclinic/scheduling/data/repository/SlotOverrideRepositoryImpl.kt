package ke.co.smartroundclinic.scheduling.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.SlotOverrideEntity
import ke.co.smartroundclinic.scheduling.domain.repository.SlotOverrideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class SlotOverrideRepositoryImpl(database: MongoDatabase) : SlotOverrideRepository {

    private val log = LoggerFactory.getLogger(SlotOverrideRepositoryImpl::class.java)
    private val col = database.getCollection<SlotOverrideEntity>(MongoDBConstants.SLOT_OVERRIDES)

    override suspend fun create(entity: SlotOverrideEntity): Resource<SlotOverrideEntity?> =
        withContext(Dispatchers.IO) {
            try {
                col.insertOne(entity)
                log.info("Created slot override id=${entity.id} for doctorId=${entity.doctorId} date=${entity.date}")
                Resource.Success(data = entity, message = "Slot override created successfully")
            } catch (e: Exception) {
                log.error("Failed to create slot override — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to create slot override")
            }
        }

    override suspend fun getByDoctorAndDate(doctorId: String, date: String): Resource<List<SlotOverrideEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val items = col.find(
                    Filters.and(
                        Filters.eq(SlotOverrideEntity::doctorId.name, doctorId),
                        Filters.eq(SlotOverrideEntity::date.name, date),
                    )
                ).toList()
                Resource.Success(data = items, message = "Slot overrides retrieved successfully")
            } catch (e: Exception) {
                log.error("Failed to fetch overrides for doctorId=$doctorId date=$date — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch slot overrides")
            }
        }

    override suspend fun getByDoctorAndDateRange(
        doctorId: String,
        from: String,
        to: String,
    ): Resource<List<SlotOverrideEntity>> = withContext(Dispatchers.IO) {
        try {
            val items = col.find(
                Filters.and(
                    Filters.eq(SlotOverrideEntity::doctorId.name, doctorId),
                    Filters.gte(SlotOverrideEntity::date.name, from),
                    Filters.lte(SlotOverrideEntity::date.name, to),
                )
            ).toList()
            Resource.Success(data = items, message = "Slot overrides retrieved successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch slot overrides")
        }
    }

    override suspend fun delete(id: String): Resource<Nothing?> =
        withContext(Dispatchers.IO) {
            try {
                val result = col.deleteOne(Filters.eq(SlotOverrideEntity::id.name, id))
                if (result.deletedCount == 0L) return@withContext Resource.Error("Slot override not found")
                log.info("Deleted slot override id=$id")
                Resource.Success(data = null, message = "Slot override deleted successfully")
            } catch (e: Exception) {
                log.error("Failed to delete slot override id=$id — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to delete slot override")
            }
        }
}
