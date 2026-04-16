package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerCertificationsEntity
import ke.co.smartroundclinic.doctor.data.entity.PractitionerLicenceEntity
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory

class PractitionerLicenceRepositoryImpl(database: MongoDatabase) : PractitionerLicenceRepository {

    private val log = LoggerFactory.getLogger(PractitionerLicenceRepositoryImpl::class.java)
    private val col = database.getCollection<PractitionerLicenceEntity>(MongoDBConstants.DOCTOR_LICENCES)

    override suspend fun add(entity: PractitionerLicenceEntity): Resource<PractitionerLicenceEntity> = try {
        val existingLicence = col.find(Filters.eq(PractitionerLicenceEntity::licenceName.name, entity.licenceName)).firstOrNull()
        if (existingLicence != null) {
            log.warn("Licence already exists for doctorId=${entity.doctorId}")
            return Resource.Error("Licence already exists")
        }
        col.insertOne(entity)
        log.info("Licence added for doctorId=${entity.doctorId}")
        Resource.Success(entity, "Licence added successfully")
    } catch (e: Exception) {
        log.error("Failed to add licence for doctorId=${entity.doctorId} — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to add licence")
    }

    override suspend fun update(
        id: String,
        doctorId: String,
        licenceName: String?,
    ): Resource<PractitionerLicenceEntity?> = try {
        val fields = mutableListOf<Bson>()
        licenceName?.let { fields.add(Updates.set(PractitionerLicenceEntity::licenceName.name, it)) }
        fields.add(Updates.set(PractitionerLicenceEntity::updatedAt.name, System.currentTimeMillis()))

        val updated = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(PractitionerLicenceEntity::id.name, id),
                Filters.eq(PractitionerLicenceEntity::doctorId.name, doctorId),
            ),
            Updates.combine(fields),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No licence found id=$id for doctorId=$doctorId")
        else log.info("Licence id=$id updated for doctorId=$doctorId")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to update licence id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update licence")
    }

    override suspend fun delete(id: String, doctorId: String): Resource<Boolean> = try {
        val result = col.deleteOne(
            Filters.and(
                Filters.eq(PractitionerLicenceEntity::id.name, id),
                Filters.eq(PractitionerLicenceEntity::doctorId.name, doctorId),
            )
        )
        val deleted = result.deletedCount > 0
        if (deleted) log.info("Licence id=$id deleted for doctorId=$doctorId")
        else log.warn("No licence found to delete id=$id for doctorId=$doctorId")
        Resource.Success(deleted)
    } catch (e: Exception) {
        log.error("Failed to delete licence id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to delete licence")
    }

    override suspend fun getAll(doctorId: String): Resource<List<PractitionerLicenceEntity>> = try {
        val items = col.find(Filters.eq(PractitionerLicenceEntity::doctorId.name, doctorId)).toList()
        Resource.Success(items)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch licences")
    }

    override suspend fun getById(id: String, doctorId: String): Resource<PractitionerLicenceEntity?> = try {

        val result = col.find(
            Filters.and(
                Filters.eq(PractitionerLicenceEntity::id.name, id),
                Filters.eq(PractitionerLicenceEntity::doctorId.name, doctorId),
            )
        ).firstOrNull() ?: return Resource.Error("No licence found")
        Resource.Success(result)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch licence")
    }

    override suspend fun updateUrl(
        id: String,
        doctorId: String,
        url: String?
    ): Resource<PractitionerLicenceEntity?> = try {
        val licence = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(PractitionerCertificationsEntity::id.name, id),
                Filters.eq(PractitionerCertificationsEntity::doctorId.name, doctorId),
            ),
            Updates.set(PractitionerCertificationsEntity::certificationUrl.name, url),
        ) ?: return Resource.Error("No certification found to update url")
        log.info("Certification url updated for id=$id")
        Resource.Success(licence.copy(licenceUrl = url))
    } catch (e: Exception) {
        log.error("Failed to update certification url id=$id — ${e.message}", e.message)
        Resource.Error(e.message ?: "Failed to update certification url")
    }
}
