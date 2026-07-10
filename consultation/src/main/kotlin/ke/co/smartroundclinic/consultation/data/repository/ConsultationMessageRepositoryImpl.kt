package ke.co.smartroundclinic.consultation.data.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.MessageCursor
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
                .sort(Sorts.ascending(ConsultationMessageEntity::createdAt.name, ConsultationMessageEntity::id.name))
                .skip((safePage - 1) * safeSize)
                .limit(safeSize)
                .toList()
            Resource.Success(items to total)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch messages")
        }
    }

    override suspend fun getByThread(
        doctorId: String,
        patientId: String,
        before: MessageCursor?,
        size: Int,
    ): Resource<List<ConsultationMessageEntity>> = withContext(Dispatchers.IO) {
        try {
            val safeSize = minOf(maxOf(1, size), 100)
            val pairFilter = Filters.and(
                Filters.eq(ConsultationMessageEntity::doctorId.name, doctorId),
                Filters.eq(ConsultationMessageEntity::patientId.name, patientId),
            )
            val filter = if (before == null) pairFilter else Filters.and(
                pairFilter,
                Filters.or(
                    Filters.lt(ConsultationMessageEntity::createdAt.name, before.createdAt),
                    Filters.and(
                        Filters.eq(ConsultationMessageEntity::createdAt.name, before.createdAt),
                        Filters.lt(ConsultationMessageEntity::id.name, before.id),
                    ),
                ),
            )
            val items = col.find(filter)
                .sort(Sorts.orderBy(
                    Sorts.descending(ConsultationMessageEntity::createdAt.name),
                    Sorts.descending(ConsultationMessageEntity::id.name),
                ))
                .limit(safeSize)
                .toList()
            Resource.Success(items)
        } catch (e: Exception) {
            log.error("Failed to fetch thread messages for doctorId=$doctorId patientId=$patientId — ${e.message}", e)
            Resource.Error(e.message ?: "Failed to fetch conversation")
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

    private fun missingThreadFieldsFilter() = Filters.or(
        Filters.exists(ConsultationMessageEntity::doctorId.name, false),
        Filters.eq(ConsultationMessageEntity::doctorId.name, ""),
    )

    override suspend fun countMissingThreadFields(): Long = withContext(Dispatchers.IO) {
        try {
            col.countDocuments(missingThreadFieldsFilter())
        } catch (e: Exception) {
            log.error("Failed to count messages missing thread fields — ${e.message}", e)
            0L
        }
    }

    override suspend fun backfillThreadFields(consultationId: String, doctorId: String, patientId: String): Long =
        withContext(Dispatchers.IO) {
            try {
                val filter = Filters.and(
                    Filters.eq(ConsultationMessageEntity::consultationId.name, consultationId),
                    missingThreadFieldsFilter(),
                )
                val update = Updates.combine(
                    Updates.set(ConsultationMessageEntity::doctorId.name, doctorId),
                    Updates.set(ConsultationMessageEntity::patientId.name, patientId),
                )
                col.updateMany(filter, update).modifiedCount
            } catch (e: Exception) {
                log.error("Failed to backfill thread fields for consultationId=$consultationId — ${e.message}", e)
                0L
            }
        }

    override suspend fun getUserName(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            users.find(Filters.eq("id", userId)).firstOrNull()?.getString("fullName")
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getUserInfo(userId: String): Pair<String, String?>? = withContext(Dispatchers.IO) {
        try {
            val doc = users.find(Filters.eq("id", userId)).firstOrNull() ?: return@withContext null
            (doc.getString("fullName") ?: "Unknown") to doc.getString("profilePicture")
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getLatestForThread(doctorId: String, patientId: String): ConsultationMessageEntity? =
        withContext(Dispatchers.IO) {
            try {
                col.find(
                    Filters.and(
                        Filters.eq(ConsultationMessageEntity::doctorId.name, doctorId),
                        Filters.eq(ConsultationMessageEntity::patientId.name, patientId),
                    )
                )
                    .sort(Sorts.orderBy(
                        Sorts.descending(ConsultationMessageEntity::createdAt.name),
                        Sorts.descending(ConsultationMessageEntity::id.name),
                    ))
                    .limit(1)
                    .firstOrNull()
            } catch (_: Exception) {
                null
            }
        }
}
