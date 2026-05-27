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
import kotlin.time.Clock

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
        val now = Clock.System.now().toString()
        val entity = col.find(Filters.eq(ComplianceEntity::id.name, id)).firstOrNull()
            ?: return Resource.Error("No compliance record found with id=$id")
        if (entity.isApproved) return Resource.Error("Compliance record already approved")

        val updated = col.findOneAndUpdate(
            Filters.eq(ComplianceEntity::id.name, id),
            Updates.combine(
                Updates.set(ComplianceEntity::isApproved.name, true),
                Updates.set(ComplianceEntity::isMonetized.name, true),
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
        val now = Clock.System.now().toString()
        val entity = col.find(Filters.eq(ComplianceEntity::id.name, id)).firstOrNull()
            ?: return Resource.Error("No compliance record found with id=$id")
        if (entity.isApproved) return Resource.Error("Compliance record is already approved")
        if (entity.approvedBy != null) return Resource.Error("Compliance record is already rejected")

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

    override suspend fun getAll(page: Int, size: Int, status: String?, doctorIds: Set<String>?): Resource<Pair<List<ComplianceEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)

        val filters = mutableListOf<org.bson.conversions.Bson>()
        when (status?.uppercase()) {
            "APPROVED" -> filters.add(Filters.eq(ComplianceEntity::isApproved.name, true))
            "REJECTED" -> {
                filters.add(Filters.eq(ComplianceEntity::isApproved.name, false))
                filters.add(Filters.ne(ComplianceEntity::approvedBy.name, null))
            }
            "PENDING" -> {
                filters.add(Filters.eq(ComplianceEntity::isApproved.name, false))
                filters.add(Filters.eq(ComplianceEntity::approvedBy.name, null))
            }
        }
        if (!doctorIds.isNullOrEmpty()) {
            filters.add(Filters.`in`(ComplianceEntity::doctorId.name, doctorIds))
        }

        val query = if (filters.isEmpty()) col.find() else col.find(Filters.and(filters))
        val total = if (filters.isEmpty()) col.countDocuments() else col.countDocuments(Filters.and(filters))
        val items = query.skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch compliance records")
    }

    override suspend fun setMonetization(id: String, isMonetized: Boolean): Resource<ComplianceEntity?> = try {
        val entity = col.find(Filters.eq(ComplianceEntity::id.name, id)).firstOrNull()
            ?: return Resource.Error("No compliance record found with id=$id")
        if (!entity.isApproved) return Resource.Error("Doctor must be approved before monetization can be changed")
        val updated = col.findOneAndUpdate(
            Filters.eq(ComplianceEntity::id.name, id),
            Updates.combine(
                Updates.set(ComplianceEntity::isMonetized.name, isMonetized),
                Updates.set(ComplianceEntity::updatedAt.name, Clock.System.now().toString()),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        log.info("Compliance id=$id monetization set to $isMonetized")
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to set monetization for compliance id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update monetization status")
    }
}
