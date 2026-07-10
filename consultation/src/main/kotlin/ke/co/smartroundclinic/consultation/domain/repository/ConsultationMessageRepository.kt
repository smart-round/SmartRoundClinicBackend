package ke.co.smartroundclinic.consultation.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import kotlinx.coroutines.flow.Flow

/** Opaque pagination cursor for [ConsultationMessageRepository.getByThread] — the (createdAt, id) of the oldest message loaded so far. */
data class MessageCursor(val createdAt: String, val id: String)

interface ConsultationMessageRepository {
    suspend fun save(entity: ConsultationMessageEntity): Resource<ConsultationMessageEntity>

    /** Merged, cursor-paginated history for a permanent (doctorId, patientId) thread, newest first. */
    suspend fun getByThread(doctorId: String, patientId: String, before: MessageCursor?, size: Int): Resource<List<ConsultationMessageEntity>>
    fun watchMessagesForThread(doctorId: String, patientId: String): Flow<ConsultationMessageEntity>
    suspend fun getUserName(userId: String): String?

    /** Display name + profile picture URL, for rendering a conversation thread's counterpart. */
    suspend fun getUserInfo(userId: String): Pair<String, String?>?

    /** The user's persisted last-seen timestamp (set when their presence socket disconnects), null if never recorded. */
    suspend fun getLastSeenAt(userId: String): String?

    /** Most recent message for a (doctorId, patientId) pair — used for the thread-list preview. */
    suspend fun getLatestForThread(doctorId: String, patientId: String): ConsultationMessageEntity?
}
