package ke.co.smartroundclinic.doctorchat.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatThreadEntity
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

class DoctorChatThreadRepositoryImpl(doctorChatDb: MongoDatabase) : DoctorChatThreadRepository {

    private val log = LoggerFactory.getLogger(DoctorChatThreadRepositoryImpl::class.java)
    private val col = doctorChatDb.getCollection<DoctorChatThreadEntity>(MongoDBConstants.DOCTOR_CHAT_THREADS)

    private fun sorted(doctorAId: String, doctorBId: String) =
        if (doctorAId <= doctorBId) doctorAId to doctorBId else doctorBId to doctorAId

    private fun pairFilter(doctorAId: String, doctorBId: String): org.bson.conversions.Bson {
        val (a, b) = sorted(doctorAId, doctorBId)
        return Filters.and(
            Filters.eq(DoctorChatThreadEntity::doctorAId.name, a),
            Filters.eq(DoctorChatThreadEntity::doctorBId.name, b),
        )
    }

    override suspend fun getOrCreate(doctorAId: String, doctorBId: String): Resource<DoctorChatThreadEntity> = try {
        val existing = col.find(pairFilter(doctorAId, doctorBId)).firstOrNull()
        if (existing != null) {
            Resource.Success(existing)
        } else {
            val (a, b) = sorted(doctorAId, doctorBId)
            val entity = DoctorChatThreadEntity(doctorAId = a, doctorBId = b)
            col.insertOne(entity)
            log.info("Created doctor-chat thread id=${entity.id} between doctorAId=$a doctorBId=$b")
            Resource.Success(entity)
        }
    } catch (e: Exception) {
        log.error("Failed to get or create doctor-chat thread for $doctorAId/$doctorBId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to load thread")
    }

    override suspend fun getById(threadId: String): Resource<DoctorChatThreadEntity?> = try {
        Resource.Success(col.find(Filters.eq(DoctorChatThreadEntity::id.name, threadId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch thread")
    }

    override suspend fun getByVideoRoomId(videoRoomId: String): Resource<DoctorChatThreadEntity?> = try {
        Resource.Success(col.find(Filters.eq(DoctorChatThreadEntity::videoRoomId.name, videoRoomId)).firstOrNull())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch thread by videoRoomId")
    }

    override suspend fun getThreadsForDoctor(doctorId: String): Resource<List<DoctorChatThreadEntity>> = try {
        val filter = Filters.or(
            Filters.eq(DoctorChatThreadEntity::doctorAId.name, doctorId),
            Filters.eq(DoctorChatThreadEntity::doctorBId.name, doctorId),
        )
        Resource.Success(col.find(filter).toList())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch threads")
    }

    override suspend fun setVideoRoomId(threadId: String, videoRoomId: String): Resource<DoctorChatThreadEntity?> = try {
        val updated = col.findOneAndUpdate(
            Filters.eq(DoctorChatThreadEntity::id.name, threadId),
            Updates.combine(
                Updates.set(DoctorChatThreadEntity::videoRoomId.name, videoRoomId),
                Updates.set(DoctorChatThreadEntity::updatedAt.name, sortableNowIso()),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        Resource.Success(updated)
    } catch (e: Exception) {
        log.error("Failed to set videoRoomId for threadId=$threadId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update thread")
    }

    override suspend fun setVideoRoomIdIfAbsent(threadId: String, videoRoomId: String): Resource<String> = try {
        val updated = col.findOneAndUpdate(
            Filters.and(
                Filters.eq(DoctorChatThreadEntity::id.name, threadId),
                Filters.or(
                    Filters.exists(DoctorChatThreadEntity::videoRoomId.name, false),
                    Filters.eq(DoctorChatThreadEntity::videoRoomId.name, null),
                ),
            ),
            Updates.combine(
                Updates.set(DoctorChatThreadEntity::videoRoomId.name, videoRoomId),
                Updates.set(DoctorChatThreadEntity::updatedAt.name, sortableNowIso()),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
        if (updated != null) {
            Resource.Success(videoRoomId)
        } else {
            val current = col.find(Filters.eq(DoctorChatThreadEntity::id.name, threadId)).firstOrNull()
            val existingId = current?.videoRoomId
            if (!existingId.isNullOrBlank()) Resource.Success(existingId) else Resource.Success(videoRoomId)
        }
    } catch (e: Exception) {
        log.error("Failed to setVideoRoomIdIfAbsent for threadId=$threadId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to set videoRoomId")
    }

    override suspend fun clearVideoRoomId(threadId: String, completedRoomId: String?): Resource<Unit> = try {
        val updates = mutableListOf(
            Updates.unset(DoctorChatThreadEntity::videoRoomId.name),
            Updates.set(DoctorChatThreadEntity::updatedAt.name, sortableNowIso()),
        )
        if (completedRoomId != null) updates.add(Updates.set(DoctorChatThreadEntity::lastVideoRoomId.name, completedRoomId))
        col.updateOne(Filters.eq(DoctorChatThreadEntity::id.name, threadId), Updates.combine(updates))
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to clear videoRoomId for threadId=$threadId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to clear videoRoomId")
    }
}
