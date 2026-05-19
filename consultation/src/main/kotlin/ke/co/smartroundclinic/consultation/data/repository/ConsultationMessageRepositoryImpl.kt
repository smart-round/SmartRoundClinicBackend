package ke.co.smartroundclinic.consultation.data.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import org.slf4j.LoggerFactory

class ConsultationMessageRepositoryImpl(
    consultationDb: MongoDatabase,
    authDb: MongoDatabase,
) : ConsultationMessageRepository {

    private val log = LoggerFactory.getLogger(ConsultationMessageRepositoryImpl::class.java)
    private val col = consultationDb.getCollection<ConsultationMessageEntity>(MongoDBConstants.CONSULTATION_MESSAGES)
    private val users = authDb.getCollection<Document>(MongoDBConstants.AUTH_USER)

    override suspend fun save(entity: ConsultationMessageEntity): Resource<ConsultationMessageEntity> =
        withContext(Dispatchers.IO) {
            try {
                col.insertOne(entity)
                Resource.Success(entity, "Message sent")
            } catch (e: Exception) {
                log.error("Failed to save consultation message — ${e.message}", e)
                Resource.Error(e.message ?: "Failed to send message")
            }
        }

    override suspend fun getByConsultationId(
        consultationId: String,
        page: Int,
        size: Int,
    ): Resource<Pair<List<ConsultationMessageEntity>, Long>> = withContext(Dispatchers.IO) {
        try {
            val safePage = maxOf(1, page)
            val safeSize = minOf(maxOf(1, size), 100)
            val filter = Filters.eq(ConsultationMessageEntity::consultationId.name, consultationId)
            val total = col.countDocuments(filter)
            val items = col.find(filter)
                .skip((safePage - 1) * safeSize)
                .limit(safeSize)
                .toList()
            Resource.Success(items to total)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch messages")
        }
    }

    override fun watchMessages(consultationId: String): Flow<ConsultationMessageEntity> =
        col.watch(
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("operationType", "insert"),
                        Filters.eq("fullDocument.consultationId", consultationId),
                    )
                )
            )
        ).mapNotNull { it.fullDocument }

    override suspend fun getUserName(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            users.find(Filters.eq("id", userId)).firstOrNull()?.getString("fullName")
        } catch (_: Exception) {
            null
        }
    }
}
