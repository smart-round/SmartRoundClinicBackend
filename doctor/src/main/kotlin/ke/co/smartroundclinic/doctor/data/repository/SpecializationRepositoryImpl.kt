package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.SpecializationEntity
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

class SpecializationRepositoryImpl(database: MongoDatabase) : SpecializationRepository {

    private val log = LoggerFactory.getLogger(SpecializationRepositoryImpl::class.java)
    private val col = database.getCollection<SpecializationEntity>(MongoDBConstants.DOCTOR_SPECIALIZATIONS)

    override suspend fun add(entity: SpecializationEntity): Resource<SpecializationEntity> = try {
        val existing = col.find(
            Filters.and(
                Filters.eq(SpecializationEntity::doctorId.name, entity.doctorId),
                Filters.eq(SpecializationEntity::specializationId.name, entity.specializationId),
            )
        ).toList()
        if (existing.isNotEmpty()) {
            log.warn("Specialization ${entity.specializationId} already added for doctorId=${entity.doctorId}")
            return Resource.Error("This specialization has already been added")
        }
        col.insertOne(entity)
        log.info("Specialization ${entity.specializationId} added for doctorId=${entity.doctorId}")
        Resource.Success(entity, "Specialization added successfully")
    } catch (e: Exception) {
        log.error("Failed to add specialization for doctorId=${entity.doctorId} — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to add specialization")
    }

    override suspend fun remove(id: String, doctorId: String): Resource<Boolean> = try {
        val result = col.deleteOne(
            Filters.and(
                Filters.eq(SpecializationEntity::id.name, id),
                Filters.eq(SpecializationEntity::doctorId.name, doctorId),
            )
        )
        val deleted = result.deletedCount > 0
        if (deleted) log.info("Specialization id=$id removed for doctorId=$doctorId")
        else log.warn("No specialization found to remove id=$id for doctorId=$doctorId")
        Resource.Success(deleted)
    } catch (e: Exception) {
        log.error("Failed to remove specialization id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to remove specialization")
    }

    override suspend fun getByDoctorId(doctorId: String): Resource<List<SpecializationEntity>> = try {
        Resource.Success(
            col.find(Filters.eq(SpecializationEntity::doctorId.name, doctorId)).toList()
        )
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch specializations")
    }
}
