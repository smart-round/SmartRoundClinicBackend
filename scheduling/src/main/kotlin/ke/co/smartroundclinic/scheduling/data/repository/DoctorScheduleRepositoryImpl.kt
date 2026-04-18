package ke.co.smartroundclinic.scheduling.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.BreakBlockDoc
import ke.co.smartroundclinic.scheduling.data.entity.DoctorScheduleEntity
import ke.co.smartroundclinic.scheduling.domain.model.BreakBlock
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class DoctorScheduleRepositoryImpl(database: MongoDatabase) : DoctorScheduleRepository {

    private val log = LoggerFactory.getLogger(DoctorScheduleRepositoryImpl::class.java)
    private val col = database.getCollection<DoctorScheduleEntity>(MongoDBConstants.DOCTOR_SCHEDULES)

    override suspend fun upsert(entity: DoctorScheduleEntity): Resource<DoctorScheduleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val filter = Filters.and(
                    Filters.eq(DoctorScheduleEntity::doctorId.name, entity.doctorId),
                    Filters.eq(DoctorScheduleEntity::dayOfWeek.name, entity.dayOfWeek),
                )
                val existing = col.find(filter).firstOrNull()
                val toSave = if (existing != null) {
                    entity.copy(id = existing.id, createdAt = existing.createdAt, updatedAt = Clock.System.now().toString())
                } else entity
                col.replaceOne(filter, toSave, ReplaceOptions().upsert(true))
                log.info("Upserted schedule for doctorId=${entity.doctorId} day=${entity.dayOfWeek}")
                Resource.Success(data = toSave, message = "Schedule saved successfully")
            } catch (e: Exception) {
                log.error("Failed to upsert schedule — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to save schedule")
            }
        }

    override suspend fun getByDoctorAndDay(doctorId: String, dayOfWeek: Int): Resource<DoctorScheduleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = col.find(
                    Filters.and(
                        Filters.eq(DoctorScheduleEntity::doctorId.name, doctorId),
                        Filters.eq(DoctorScheduleEntity::dayOfWeek.name, dayOfWeek),
                    )
                ).firstOrNull()
                Resource.Success(data = entity, message = "Schedule retrieved")
            } catch (e: Exception) {
                log.error("Failed to fetch schedule for doctorId=$doctorId day=$dayOfWeek — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch schedule")
            }
        }

    override suspend fun getByDoctor(doctorId: String): Resource<List<DoctorScheduleEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val items = col.find(Filters.eq(DoctorScheduleEntity::doctorId.name, doctorId)).toList()
                Resource.Success(data = items, message = "Schedule retrieved successfully")
            } catch (e: Exception) {
                log.error("Failed to fetch schedule for doctorId=$doctorId — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to fetch schedule")
            }
        }

    override suspend fun update(
        doctorId: String,
        dayOfWeek: Int,
        windowStart: String?,
        windowEnd: String?,
        slotDuration: Int?,
        breakBlocks: List<BreakBlock>?,
        isActive: Boolean?,
        timezone: String?,
    ): Resource<DoctorScheduleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val filter = Filters.and(
                    Filters.eq(DoctorScheduleEntity::doctorId.name, doctorId),
                    Filters.eq(DoctorScheduleEntity::dayOfWeek.name, dayOfWeek),
                )
                col.find(filter).firstOrNull()
                    ?: return@withContext Resource.Error("Schedule not found for day $dayOfWeek")

                val updates = mutableListOf<Bson>()
                windowStart?.let { updates.add(Updates.set(DoctorScheduleEntity::windowStart.name, it)) }
                windowEnd?.let { updates.add(Updates.set(DoctorScheduleEntity::windowEnd.name, it)) }
                slotDuration?.let { updates.add(Updates.set(DoctorScheduleEntity::slotDuration.name, it)) }
                breakBlocks?.let {
                    updates.add(Updates.set(DoctorScheduleEntity::breakBlocks.name, it.map { b -> BreakBlockDoc(b.start, b.end) }))
                }
                isActive?.let { updates.add(Updates.set(DoctorScheduleEntity::isActive.name, it)) }
                timezone?.let { updates.add(Updates.set(DoctorScheduleEntity::timezone.name, it)) }

                if (updates.isEmpty()) return@withContext Resource.Error("No changes provided")

                updates.add(Updates.set(DoctorScheduleEntity::updatedAt.name, Clock.System.now().toString()))
                col.updateOne(filter, Updates.combine(updates))
                val updated = col.find(filter).firstOrNull()
                log.info("Updated schedule for doctorId=$doctorId day=$dayOfWeek")
                Resource.Success(data = updated, message = "Schedule updated successfully")
            } catch (e: Exception) {
                log.error("Failed to update schedule — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to update schedule")
            }
        }

    override suspend fun deactivate(doctorId: String, dayOfWeek: Int): Resource<DoctorScheduleEntity?> =
        withContext(Dispatchers.IO) {
            try {
                val filter = Filters.and(
                    Filters.eq(DoctorScheduleEntity::doctorId.name, doctorId),
                    Filters.eq(DoctorScheduleEntity::dayOfWeek.name, dayOfWeek),
                )
                col.find(filter).firstOrNull()
                    ?: return@withContext Resource.Error("Schedule not found for day $dayOfWeek")
                col.updateOne(
                    filter,
                    Updates.combine(
                        Updates.set(DoctorScheduleEntity::isActive.name, false),
                        Updates.set(DoctorScheduleEntity::updatedAt.name, Clock.System.now().toString()),
                    )
                )
                val updated = col.find(filter).firstOrNull()
                log.info("Deactivated schedule for doctorId=$doctorId day=$dayOfWeek")
                Resource.Success(data = updated, message = "Schedule deactivated successfully")
            } catch (e: Exception) {
                log.error("Failed to deactivate schedule — ${e.message}", e)
                Resource.Error(e.localizedMessage ?: "Failed to deactivate schedule")
            }
        }
}
