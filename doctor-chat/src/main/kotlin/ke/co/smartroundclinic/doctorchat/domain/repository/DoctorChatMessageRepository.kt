package ke.co.smartroundclinic.doctorchat.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatMessageEntity
import kotlinx.coroutines.flow.Flow

/** Opaque pagination cursor — the (createdAt, id) of the oldest message loaded so far. */
data class DoctorChatMessageCursor(val createdAt: String, val id: String)

interface DoctorChatMessageRepository {
    suspend fun save(entity: DoctorChatMessageEntity): Resource<DoctorChatMessageEntity>
    suspend fun getByThread(threadId: String, before: DoctorChatMessageCursor?, size: Int): Resource<List<DoctorChatMessageEntity>>
    fun watchMessagesForThread(threadId: String): Flow<DoctorChatMessageEntity>
    suspend fun getLatestForThread(threadId: String): DoctorChatMessageEntity?
    suspend fun getUserName(userId: String): String?

    /** Display name + presigned profile picture URL. */
    suspend fun getUserInfo(userId: String): Pair<String, String?>?
}
