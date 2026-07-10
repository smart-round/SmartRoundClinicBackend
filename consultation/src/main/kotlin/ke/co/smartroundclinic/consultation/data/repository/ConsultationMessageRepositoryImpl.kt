package ke.co.smartroundclinic.consultation.data.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.MessageCursor
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import org.slf4j.LoggerFactory

private const val PROFILE_PICTURE_URL_TTL = 86400L  // 24 hours

class ConsultationMessageRepositoryImpl(
    consultationDb: MongoDatabase,
    authDb: MongoDatabase,
    private val storageRepository: StorageRepository,
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

    override fun watchMessagesForThread(doctorId: String, patientId: String): Flow<ConsultationMessageEntity> =
        col.watch(
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("operationType", "insert"),
                        Filters.eq("fullDocument.doctorId", doctorId),
                        Filters.eq("fullDocument.patientId", patientId),
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

    override suspend fun getUserInfo(userId: String): Pair<String, String?>? = withContext(Dispatchers.IO) {
        try {
            val doc = users.find(Filters.eq("id", userId)).firstOrNull() ?: return@withContext null
            val name = doc.getString("fullName") ?: "Unknown"
            // profilePicture is a storage key, not a fetchable URL — presign it like every other call site does.
            val key = doc.getString("profilePicture")
            val picture = if (key.isNullOrBlank()) null else
                (storageRepository.presignedGetUrl(AppConfig.r2.bucket, key, PROFILE_PICTURE_URL_TTL) as? Resource.Success)?.data
            name to picture
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

    override suspend fun getLastSeenAt(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            users.find(Filters.eq("id", userId)).firstOrNull()?.getString("lastSeenAt")
        } catch (_: Exception) {
            null
        }
    }
}
