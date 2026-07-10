package ke.co.smartroundclinic.consultation.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.consultation.data.entity.ConsultationThreadEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadRepository
import kotlinx.coroutines.flow.firstOrNull
import org.slf4j.LoggerFactory

class ConsultationThreadRepositoryImpl(
    consultationDb: MongoDatabase,
) : ConsultationThreadRepository {

    private val log = LoggerFactory.getLogger(ConsultationThreadRepositoryImpl::class.java)
    private val col = consultationDb.getCollection<ConsultationThreadEntity>(MongoDBConstants.CONSULTATION_THREADS)

    private fun pairFilter(doctorId: String, patientId: String) = Filters.and(
        Filters.eq(ConsultationThreadEntity::doctorId.name, doctorId),
        Filters.eq(ConsultationThreadEntity::patientId.name, patientId),
    )

    override suspend fun getOrCreate(doctorId: String, patientId: String): Resource<ConsultationThreadEntity> = try {
        val existing = col.find(pairFilter(doctorId, patientId)).firstOrNull()
        if (existing != null) {
            Resource.Success(existing)
        } else {
            val entity = ConsultationThreadEntity(doctorId = doctorId, patientId = patientId)
            col.insertOne(entity)
            Resource.Success(entity)
        }
    } catch (e: Exception) {
        log.error("Failed to get or create thread for doctorId=$doctorId patientId=$patientId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to load thread")
    }

    override suspend fun getByVideoRoomId(videoRoomId: String): Resource<ConsultationThreadEntity?> = try {
        Resource.Success(col.find(Filters.eq(ConsultationThreadEntity::videoRoomId.name, videoRoomId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch thread by videoRoomId")
    }

    override suspend fun setVideoRoomId(doctorId: String, patientId: String, videoRoomId: String): Resource<ConsultationThreadEntity?> = try {
        val updated = col.findOneAndUpdate(
            pairFilter(doctorId, patientId),
            Updates.combine(
                Updates.set(ConsultationThreadEntity::videoRoomId.name, videoRoomId),
                Updates.set(ConsultationThreadEntity::updatedAt.name, sortableNowIso()),
            ),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
        )
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to set videoRoomId for doctorId=$doctorId patientId=$patientId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update thread")
    }

    override suspend fun setVideoRoomIdIfAbsent(doctorId: String, patientId: String, videoRoomId: String): Resource<String> = try {
        val updated = col.findOneAndUpdate(
            Filters.and(
                pairFilter(doctorId, patientId),
                Filters.or(
                    Filters.exists(ConsultationThreadEntity::videoRoomId.name, false),
                    Filters.eq(ConsultationThreadEntity::videoRoomId.name, null),
                ),
            ),
            Updates.combine(
                Updates.set(ConsultationThreadEntity::videoRoomId.name, videoRoomId),
                Updates.set(ConsultationThreadEntity::updatedAt.name, sortableNowIso()),
            ),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
        )
        if (updated != null) {
            // This request won the race — our meeting ID is now stored
            Resource.Success(videoRoomId)
        } else {
            // Another request already stored a meeting ID — read and reuse it
            val current = col.find(pairFilter(doctorId, patientId)).firstOrNull()
            val existingId = current?.videoRoomId
            if (!existingId.isNullOrBlank()) Resource.Success(existingId)
            else Resource.Success(videoRoomId)
        }
    } catch (e: Exception) {
        log.error("Failed to setVideoRoomIdIfAbsent for doctorId=$doctorId patientId=$patientId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to set videoRoomId")
    }

    override suspend fun clearVideoRoomId(doctorId: String, patientId: String, completedRoomId: String?): Resource<Unit> = try {
        val updates = mutableListOf(
            Updates.unset(ConsultationThreadEntity::videoRoomId.name),
            Updates.set(ConsultationThreadEntity::updatedAt.name, sortableNowIso()),
        )
        if (completedRoomId != null) {
            updates.add(Updates.set(ConsultationThreadEntity::lastVideoRoomId.name, completedRoomId))
        }
        col.updateOne(pairFilter(doctorId, patientId), Updates.combine(updates))
        log.info("Cleared videoRoomId for doctorId=$doctorId patientId=$patientId${if (completedRoomId != null) " (lastVideoRoomId=$completedRoomId)" else ""}")
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to clear videoRoomId for doctorId=$doctorId patientId=$patientId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to clear videoRoomId")
    }
}
