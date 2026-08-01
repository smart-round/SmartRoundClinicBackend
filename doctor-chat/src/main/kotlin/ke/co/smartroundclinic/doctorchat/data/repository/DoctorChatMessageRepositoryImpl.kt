package ke.co.smartroundclinic.doctorchat.data.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatMessageEntity
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageCursor
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
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

class DoctorChatMessageRepositoryImpl(
    doctorChatDb: MongoDatabase,
    authDb: MongoDatabase,
    private val storageRepository: StorageRepository,
) : DoctorChatMessageRepository {

    private val log = LoggerFactory.getLogger(DoctorChatMessageRepositoryImpl::class.java)
    private val col = doctorChatDb.getCollection<DoctorChatMessageEntity>(MongoDBConstants.DOCTOR_CHAT_MESSAGES)
    private val users = authDb.getCollection<Document>(MongoDBConstants.AUTH_USER)

    override suspend fun save(entity: DoctorChatMessageEntity): Resource<DoctorChatMessageEntity> =
        withContext(Dispatchers.IO) {
            try {
                col.insertOne(entity)
                Resource.Success(entity, "Message sent")
            } catch (e: Exception) {
                log.error("Failed to save doctor-chat message — ${e.message}", e)
                Resource.Error(e.message ?: "Failed to send message")
            }
        }

    override suspend fun getByThread(
        threadId: String,
        before: DoctorChatMessageCursor?,
        size: Int,
    ): Resource<List<DoctorChatMessageEntity>> = withContext(Dispatchers.IO) {
        try {
            val safeSize = minOf(maxOf(1, size), 100)
            val threadFilter = Filters.eq(DoctorChatMessageEntity::threadId.name, threadId)
            val filter = if (before == null) threadFilter else Filters.and(
                threadFilter,
                Filters.or(
                    Filters.lt(DoctorChatMessageEntity::createdAt.name, before.createdAt),
                    Filters.and(
                        Filters.eq(DoctorChatMessageEntity::createdAt.name, before.createdAt),
                        Filters.lt(DoctorChatMessageEntity::id.name, before.id),
                    ),
                ),
            )
            val items = col.find(filter)
                .sort(Sorts.orderBy(
                    Sorts.descending(DoctorChatMessageEntity::createdAt.name),
                    Sorts.descending(DoctorChatMessageEntity::id.name),
                ))
                .limit(safeSize)
                .toList()
            Resource.Success(items)
        } catch (e: Exception) {
            log.error("Failed to fetch messages for threadId=$threadId — ${e.message}", e)
            Resource.Error(e.message ?: "Failed to fetch conversation")
        }
    }

    override fun watchMessagesForThread(threadId: String): Flow<DoctorChatMessageEntity> =
        col.watch(
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("operationType", "insert"),
                        Filters.eq("fullDocument.threadId", threadId),
                    )
                )
            )
        ).mapNotNull { it.fullDocument }

    override suspend fun getLatestForThread(threadId: String): DoctorChatMessageEntity? = withContext(Dispatchers.IO) {
        try {
            col.find(Filters.eq(DoctorChatMessageEntity::threadId.name, threadId))
                .sort(Sorts.orderBy(
                    Sorts.descending(DoctorChatMessageEntity::createdAt.name),
                    Sorts.descending(DoctorChatMessageEntity::id.name),
                ))
                .limit(1)
                .firstOrNull()
        } catch (_: Exception) {
            null
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
            val name = doc.getString("fullName") ?: "Unknown"
            val key = doc.getString("profilePicture")
            val picture = if (key.isNullOrBlank()) null else
                (storageRepository.presignedGetUrl(AppConfig.r2.bucket, key, PROFILE_PICTURE_URL_TTL) as? Resource.Success)?.data
            name to picture
        } catch (_: Exception) {
            null
        }
    }
}
