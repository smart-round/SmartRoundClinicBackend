package ke.co.smartroundclinic.doctor.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.ComplianceCorrectionEntity
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceCorrectionRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class ComplianceCorrectionRepositoryImpl(database: MongoDatabase) : ComplianceCorrectionRepository {

    private val log = LoggerFactory.getLogger(ComplianceCorrectionRepositoryImpl::class.java)
    private val col = database.getCollection<ComplianceCorrectionEntity>(MongoDBConstants.DOCTOR_COMPLIANCE_CORRECTIONS)

    override suspend fun create(entity: ComplianceCorrectionEntity): Resource<ComplianceCorrectionEntity> = try {
        col.insertOne(entity)
        log.info("Compliance correction submitted id=${entity.id} doctorId=${entity.doctorId} complianceId=${entity.complianceId}")
        Resource.Success(entity, "Correction submitted for review")
    } catch (e: Exception) {
        log.error("Failed to save compliance correction for doctorId=${entity.doctorId} — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to submit correction")
    }

    override suspend fun hasPending(doctorId: String): Boolean = try {
        col.find(
            Filters.and(
                Filters.eq(ComplianceCorrectionEntity::doctorId.name, doctorId),
                Filters.eq(ComplianceCorrectionEntity::status.name, "PENDING"),
            )
        ).firstOrNull() != null
    } catch (e: Exception) {
        log.error("Failed to check pending correction for doctorId=$doctorId — ${e.message}", e)
        false
    }

    override suspend fun resolvePending(doctorId: String, status: String, reviewedBy: String): Resource<Unit> = try {
        val now = Clock.System.now().toString()
        col.updateMany(
            Filters.and(
                Filters.eq(ComplianceCorrectionEntity::doctorId.name, doctorId),
                Filters.eq(ComplianceCorrectionEntity::status.name, "PENDING"),
            ),
            Updates.combine(
                Updates.set(ComplianceCorrectionEntity::status.name, status),
                Updates.set(ComplianceCorrectionEntity::reviewedAt.name, now),
                Updates.set(ComplianceCorrectionEntity::reviewedBy.name, reviewedBy),
            ),
        )
        log.info("Resolved pending corrections for doctorId=$doctorId to status=$status by reviewedBy=$reviewedBy")
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to resolve pending corrections for doctorId=$doctorId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to resolve pending corrections")
    }

    override suspend fun getLatest(page: Int, size: Int, status: String?): Resource<Pair<List<ComplianceCorrectionEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = status?.let { Filters.eq(ComplianceCorrectionEntity::status.name, it.uppercase()) }
        val total = if (filter != null) col.countDocuments(filter) else col.countDocuments()
        val query = if (filter != null) col.find(filter) else col.find()
        val items = query
            .sort(Sorts.descending(ComplianceCorrectionEntity::submittedAt.name))
            .skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        log.error("Failed to fetch latest compliance corrections — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch corrections")
    }

    override suspend fun getHistoryForDoctor(doctorId: String, page: Int, size: Int): Resource<Pair<List<ComplianceCorrectionEntity>, Long>> = try {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val filter = Filters.eq(ComplianceCorrectionEntity::doctorId.name, doctorId)
        val total = col.countDocuments(filter)
        val items = col.find(filter)
            .sort(Sorts.descending(ComplianceCorrectionEntity::submittedAt.name))
            .skip((safePage - 1) * safeSize).limit(safeSize).toList()
        Resource.Success(items to total)
    } catch (e: Exception) {
        log.error("Failed to fetch correction history for doctorId=$doctorId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch correction history")
    }
}
