package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.ComplianceEntity
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

class
ComplianceRepositoryImpl(database: MongoDatabase) : ComplianceRepository {

    private val log = LoggerFactory.getLogger(ComplianceRepositoryImpl::class.java)
    private val col = database.getCollection<ComplianceEntity>(MongoDBConstants.DOCTOR_COMPLIANCE)

    override suspend fun submit(doctorId: String): Resource<ComplianceEntity> = try {
        val existing = col.find(Filters.eq(ComplianceEntity::doctorId.name, doctorId)).firstOrNull()
        if (existing != null) {
            log.warn("Compliance record already exists for doctorId=$doctorId")
            return Resource.Error("A compliance record already exists for this doctor")
        }
        val entity = ComplianceEntity(doctorId = doctorId)
        col.insertOne(entity)
        log.info("Compliance submitted for doctorId=$doctorId")
        Resource.Success(entity, "Submitted for review successfully")
    } catch (e: Exception) {
        log.error("Failed to submit compliance for doctorId=$doctorId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to submit compliance")
    }

    override suspend fun approve(id: String, adminId: String): Resource<ComplianceEntity?> = try {
        val now = System.currentTimeMillis()
        val updated = col.findOneAndUpdate(
            Filters.eq(ComplianceEntity::id.name, id),
            Updates.combine(
                Updates.set(ComplianceEntity::isApproved.name, true),
                Updates.set(ComplianceEntity::approvedBy.name, adminId),
                Updates.set(ComplianceEntity::approvedAt.name, now),
                Updates.unset(ComplianceEntity::failedApprovalReason.name),
                Updates.set(ComplianceEntity::updatedAt.name, now),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No compliance record found with id=$id")
        else log.info("Compliance id=$id approved by adminId=$adminId")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to approve compliance id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to approve compliance")
    }

    override suspend fun reject(id: String, adminId: String, reason: String): Resource<ComplianceEntity?> = try {
        val now = System.currentTimeMillis()
        val updated = col.findOneAndUpdate(
            Filters.eq(ComplianceEntity::id.name, id),
            Updates.combine(
                Updates.set(ComplianceEntity::isApproved.name, false),
                Updates.set(ComplianceEntity::approvedBy.name, adminId),
                Updates.set(ComplianceEntity::failedApprovalReason.name, reason),
                Updates.set(ComplianceEntity::updatedAt.name, now),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated == null) log.warn("No compliance record found with id=$id")
        else log.info("Compliance id=$id rejected by adminId=$adminId")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to reject compliance id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to reject compliance")
    }

    override suspend fun getByDoctorId(doctorId: String): Resource<ComplianceEntity?> = try {
        Resource.Success(col.find(Filters.eq(ComplianceEntity::doctorId.name, doctorId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch compliance status")
    }

    override suspend fun getById(id: String): Resource<ComplianceEntity?> = try {
        Resource.Success(col.find(Filters.eq(ComplianceEntity::id.name, id)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch compliance record")
    }

    override suspend fun getAll(page: Int, size: Int): Resource<Pair<List<ComplianceEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val total = col.countDocuments()
        val items = col.find()
            .skip((safePage - 1) * safeSize)
            .limit(safeSize)
            .toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch compliance records")
    }
}
