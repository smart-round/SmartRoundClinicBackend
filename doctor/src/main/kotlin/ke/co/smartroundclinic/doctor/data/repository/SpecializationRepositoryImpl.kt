package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.SpecializationEntity
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecialityInfoRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.SubSpecialityInfoRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecializationWithDetailsRes
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlin.time.Clock
import org.bson.Document
import org.slf4j.LoggerFactory

class SpecializationRepositoryImpl(
    database: MongoDatabase,
    adminDb: MongoDatabase,
) : SpecializationRepository {

    private val log = LoggerFactory.getLogger(SpecializationRepositoryImpl::class.java)
    private val col = database.getCollection<SpecializationEntity>(MongoDBConstants.DOCTOR_SPECIALIZATIONS)
    private val specialitiesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SPECIALITIES)
    private val subSpecialitiesCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_SUB_SPECIALITIES)

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

    override suspend fun update(
        id: String,
        doctorId: String,
        specializationId: String,
        subSpecializationId: String?,
    ): Resource<SpecializationEntity?> = try {
        val existing = col.find(
            Filters.and(
                Filters.eq(SpecializationEntity::id.name, id),
                Filters.eq(SpecializationEntity::doctorId.name, doctorId),
            )
        ).firstOrNull() ?: return Resource.Error("Specialization not found")
        val updates = mutableListOf(
            Updates.set(SpecializationEntity::specializationId.name, specializationId),
            Updates.set(SpecializationEntity::updatedAt.name, Clock.System.now().toString()),
        )
        if (subSpecializationId != null) {
            updates.add(Updates.set(SpecializationEntity::subSpecializationId.name, subSpecializationId))
        } else {
            updates.add(Updates.unset(SpecializationEntity::subSpecializationId.name))
        }
        val updated = col.findOneAndUpdate(
            Filters.eq(SpecializationEntity::id.name, id),
            Updates.combine(updates),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        log.info("Specialization id=$id updated for doctorId=$doctorId")
        Resource.Success(updated, "Specialization updated successfully")
    } catch (e: Exception) {
        log.error("Failed to update specialization id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update specialization")
    }

    override suspend fun getByDoctorIdWithDetails(doctorId: String): Resource<List<SpecializationWithDetailsRes>> = try {
        val entities = col.find(Filters.eq(SpecializationEntity::doctorId.name, doctorId)).toList()
        val result = entities.mapNotNull { spec ->
            val specialityDoc = specialitiesCol.find(Filters.eq("id", spec.specializationId)).firstOrNull()
                ?: return@mapNotNull null
            val specialityInfo = SpecialityInfoRes(
                id = specialityDoc.getString("id") ?: spec.specializationId,
                title = specialityDoc.getString("title") ?: "",
                description = specialityDoc.getString("description") ?: "",
                color = specialityDoc.getString("color") ?: "#FFFFFF",
                iconUrl = specialityDoc.getString("iconUrl"),
            )
            val subSpecialityInfo = spec.subSpecializationId?.let { subId ->
                subSpecialitiesCol.find(Filters.eq("id", subId)).firstOrNull()?.let { doc ->
                    SubSpecialityInfoRes(
                        id = doc.getString("id") ?: subId,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        color = doc.getString("color") ?: "#FFFFFF",
                        iconUrl = doc.getString("iconUrl"),
                    )
                }
            }
            SpecializationWithDetailsRes(
                id = spec.id,
                doctorId = spec.doctorId,
                specialization = specialityInfo,
                subSpecialization = subSpecialityInfo,
                createdAt = spec.createdAt,
                updatedAt = spec.updatedAt,
            )
        }
        Resource.Success(result)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch specializations with details")
    }
}
