package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerCertificationsEntity
import ke.co.smartroundclinic.doctor.domain.repository.CertificationRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory

class CertificationRepositoryImpl(database: MongoDatabase) : CertificationRepository {

    private val log = LoggerFactory.getLogger(CertificationRepositoryImpl::class.java)
    private val col = database.getCollection<PractitionerCertificationsEntity>(MongoDBConstants.DOCTOR_CERTIFICATIONS)

    override suspend fun add(entity: PractitionerCertificationsEntity): Resource<PractitionerCertificationsEntity> = try {
        val existing = col.find(Filters.eq(PractitionerCertificationsEntity::certificationName.name, entity.certificationName)).firstOrNull()
        if (existing != null) {
            log.warn("Certification already exists for doctorId=${entity.doctorId}")
            return Resource.Error("Certification already exists")
        }
        col.insertOne(entity)
        log.info("Certification added for doctorId=${entity.doctorId}")
        Resource.Success(entity, "Certification added successfully")
    } catch (e: Exception) {
        log.error("Failed to add certification for doctorId=${entity.doctorId} — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to add certification")
    }

    override suspend fun update(
        id: String,
        doctorId: String,
        name: String?,
        date: String?,
    ): Resource<PractitionerCertificationsEntity?> = try {
        val fields = mutableListOf<Bson>()
        name?.let { fields.add(Updates.set(PractitionerCertificationsEntity::certificationName.name, it)) }
        date?.let { fields.add(Updates.set(PractitionerCertificationsEntity::certificationDate.name, it)) }
        fields.add(Updates.set(PractitionerCertificationsEntity::updatedAt.name, System.currentTimeMillis()))

        val updated = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(PractitionerCertificationsEntity::id.name, id),
                Filters.eq(PractitionerCertificationsEntity::doctorId.name, doctorId),
            ),
            Updates.combine(fields),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No certification found id=$id for doctorId=$doctorId")
        else log.info("Certification id=$id updated for doctorId=$doctorId")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to update certification id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update certification")
    }

    override suspend fun delete(id: String, doctorId: String): Resource<Boolean> = try {
        val result = col.deleteOne(
            Filters.and(
                Filters.eq(PractitionerCertificationsEntity::id.name, id),
                Filters.eq(PractitionerCertificationsEntity::doctorId.name, doctorId),
            )
        )
        val deleted = result.deletedCount > 0
        if (deleted) log.info("Certification id=$id deleted for doctorId=$doctorId")
        else log.warn("No certification found to delete id=$id for doctorId=$doctorId")
        Resource.Success(deleted)
    } catch (e: Exception) {
        log.error("Failed to delete certification id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to delete certification")
    }

    override suspend fun getAll(doctorId: String): Resource<List<PractitionerCertificationsEntity>> = try {
        Resource.Success(
            col.find(Filters.eq(PractitionerCertificationsEntity::doctorId.name, doctorId)).toList()
        )
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch certifications")
    }

    override suspend fun getById(id: String, doctorId: String): Resource<PractitionerCertificationsEntity?> = try {
        Resource.Success(
            col.find(
                Filters.and(
                    Filters.eq(PractitionerCertificationsEntity::id.name, id),
                    Filters.eq(PractitionerCertificationsEntity::doctorId.name, doctorId),
                )
            ).firstOrNull()
        )
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch certification")
    }

    override suspend fun updateUrl(
        id: String,
        doctorId: String,
        url: String?
    ): Resource<PractitionerCertificationsEntity?> = try {
        val certification = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(PractitionerCertificationsEntity::id.name, id),
                Filters.eq(PractitionerCertificationsEntity::doctorId.name, doctorId),
            ),
            Updates.set(PractitionerCertificationsEntity::certificationUrl.name, url),
        ) ?: return Resource.Error("No certification found to update url")
        log.info("Certification url updated for id=$id")
        Resource.Success(certification.copy(certificationUrl = url))
    } catch (e: Exception) {
        log.error("Failed to update certification url id=$id — ${e.message}", e.message)
        Resource.Error(e.message ?: "Failed to update certification url")
    }
}
