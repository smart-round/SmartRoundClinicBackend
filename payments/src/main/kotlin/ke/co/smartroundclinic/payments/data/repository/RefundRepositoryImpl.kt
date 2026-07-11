package ke.co.smartroundclinic.payments.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.RefundEntity
import ke.co.smartroundclinic.payments.domain.repository.RefundRepository
import kotlinx.coroutines.flow.firstOrNull
import org.slf4j.LoggerFactory
import kotlin.time.Clock

class RefundRepositoryImpl(database: MongoDatabase) : RefundRepository {

    private val log = LoggerFactory.getLogger(RefundRepositoryImpl::class.java)
    private val col = database.getCollection<RefundEntity>(MongoDBConstants.REFUNDS)

    override suspend fun getNextPending(): Resource<RefundEntity?> = try {
        val doc = col.find(
            Filters.and(
                Filters.eq(RefundEntity::status.name, RefundEntity.RefundStatus.PENDING.name),
                Filters.eq(RefundEntity::trackingId.name, null),
            )
        ).sort(Sorts.ascending(RefundEntity::createdAt.name)).limit(1).firstOrNull()
        Resource.Success(doc)
    } catch (e: Exception) {
        log.error("Failed to fetch next pending refund — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch next pending refund")
    }

    override suspend fun getById(id: String): Resource<RefundEntity?> = try {
        Resource.Success(col.find(Filters.eq(RefundEntity::id.name, id)).firstOrNull())
    } catch (e: Exception) {
        log.error("Failed to fetch refund id=$id — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch refund")
    }

    override suspend fun markSubmitted(id: String, trackingId: String): Resource<Unit> = try {
        col.updateOne(
            Filters.eq(RefundEntity::id.name, id),
            Updates.combine(
                Updates.set(RefundEntity::trackingId.name, trackingId),
                Updates.set(RefundEntity::updatedAt.name, Clock.System.now().toString()),
            )
        )
        log.info("Refund id=$id submitted to payment provider — trackingId=$trackingId")
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to mark refund id=$id as submitted — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update refund")
    }

    override suspend fun markFailed(id: String, reason: String): Resource<Unit> = try {
        col.updateOne(
            Filters.eq(RefundEntity::id.name, id),
            Updates.combine(
                Updates.set(RefundEntity::status.name, RefundEntity.RefundStatus.FAILED.name),
                Updates.set(RefundEntity::failureReason.name, reason),
                Updates.set(RefundEntity::updatedAt.name, Clock.System.now().toString()),
            )
        )
        log.warn("Refund id=$id marked FAILED — $reason")
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to mark refund id=$id as failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update refund")
    }

    override suspend fun updateStatusByTrackingId(trackingId: String, status: String): Resource<Unit> = try {
        col.updateOne(
            Filters.eq(RefundEntity::trackingId.name, trackingId),
            Updates.combine(
                Updates.set(RefundEntity::status.name, status),
                Updates.set(RefundEntity::updatedAt.name, Clock.System.now().toString()),
            )
        )
        log.info("Refund status updated trackingId=$trackingId status=$status")
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to update refund status trackingId=$trackingId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update refund status")
    }
}
