package ke.co.smartroundclinic.consultation.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.consultation.data.entity.ConsultationHiddenThreadEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

class ConsultationHiddenThreadRepositoryImpl(
    consultationDb: MongoDatabase,
) : ConsultationHiddenThreadRepository {

    private val log = LoggerFactory.getLogger(ConsultationHiddenThreadRepositoryImpl::class.java)
    private val col = consultationDb.getCollection<ConsultationHiddenThreadEntity>(MongoDBConstants.CONSULTATION_HIDDEN_THREADS)

    override suspend fun hide(userId: String, doctorId: String, patientId: String): Resource<Unit> = try {
        col.findOneAndUpdate(
            Filters.and(
                Filters.eq(ConsultationHiddenThreadEntity::userId.name, userId),
                Filters.eq(ConsultationHiddenThreadEntity::doctorId.name, doctorId),
                Filters.eq(ConsultationHiddenThreadEntity::patientId.name, patientId),
            ),
            Updates.set(ConsultationHiddenThreadEntity::hiddenAt.name, sortableNowIso()),
            FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
        )
        Resource.Success(Unit)
    } catch (e: Exception) {
        log.error("Failed to hide thread for userId=$userId doctorId=$doctorId patientId=$patientId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to delete conversation")
    }

    override suspend fun getHiddenMap(userId: String): Resource<Map<Pair<String, String>, String>> = try {
        val docs = col.find(Filters.eq(ConsultationHiddenThreadEntity::userId.name, userId)).toList()
        Resource.Success(docs.associate { (it.doctorId to it.patientId) to it.hiddenAt })
    } catch (e: Exception) {
        log.error("Failed to fetch hidden threads for userId=$userId — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch hidden conversations")
    }
}
