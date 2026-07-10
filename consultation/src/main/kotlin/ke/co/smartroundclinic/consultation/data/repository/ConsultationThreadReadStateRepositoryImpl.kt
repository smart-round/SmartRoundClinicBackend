package ke.co.smartroundclinic.consultation.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.consultation.data.entity.ConsultationThreadReadStateEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadReadStateRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

class ConsultationThreadReadStateRepositoryImpl(
    consultationDb: MongoDatabase,
) : ConsultationThreadReadStateRepository {

    private val log = LoggerFactory.getLogger(ConsultationThreadReadStateRepositoryImpl::class.java)
    private val col = consultationDb.getCollection<ConsultationThreadReadStateEntity>(MongoDBConstants.CONSULTATION_THREAD_READ_STATE)

    private fun pairFilter(doctorId: String, patientId: String) = Filters.and(
        Filters.eq(ConsultationThreadReadStateEntity::doctorId.name, doctorId),
        Filters.eq(ConsultationThreadReadStateEntity::patientId.name, patientId),
    )

    override suspend fun getByPair(doctorId: String, patientId: String): Resource<ConsultationThreadReadStateEntity?> = try {
        Resource.Success(col.find(pairFilter(doctorId, patientId)).firstOrNull())
    } catch (e: Exception) {
        log.error("Failed to fetch read state for doctorId=$doctorId patientId=$patientId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch read state")
    }

    override suspend fun getByPairs(pairs: List<Pair<String, String>>): Resource<List<ConsultationThreadReadStateEntity>> = try {
        if (pairs.isEmpty()) return Resource.Success(emptyList())
        val filter = Filters.or(pairs.map { (doctorId, patientId) -> pairFilter(doctorId, patientId) })
        Resource.Success(col.find(filter).toList())
    } catch (e: Exception) {
        log.error("Failed to batch-fetch read state — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch read state")
    }

    override suspend fun markRead(doctorId: String, patientId: String, readerId: String, at: String): Resource<ConsultationThreadReadStateEntity?> =
        advanceWatermark(
            doctorId, patientId,
            fieldName = if (readerId == doctorId) ConsultationThreadReadStateEntity::doctorLastReadAt.name
            else ConsultationThreadReadStateEntity::patientLastReadAt.name,
            at = at,
        )

    override suspend fun bumpDelivered(doctorId: String, patientId: String, recipientId: String, at: String): Resource<ConsultationThreadReadStateEntity?> =
        advanceWatermark(
            doctorId, patientId,
            fieldName = if (recipientId == doctorId) ConsultationThreadReadStateEntity::doctorLastDeliveredAt.name
            else ConsultationThreadReadStateEntity::patientLastDeliveredAt.name,
            at = at,
        )

    private suspend fun advanceWatermark(doctorId: String, patientId: String, fieldName: String, at: String): Resource<ConsultationThreadReadStateEntity?> = try {
        val updated = col.findOneAndUpdate(
            pairFilter(doctorId, patientId),
            Updates.combine(
                Updates.max(fieldName, at),
                Updates.set(ConsultationThreadReadStateEntity::updatedAt.name, sortableNowIso()),
            ),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
        )
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to advance $fieldName for doctorId=$doctorId patientId=$patientId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update read state")
    }
}
