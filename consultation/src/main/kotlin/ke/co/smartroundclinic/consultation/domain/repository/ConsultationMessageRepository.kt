package ke.co.smartroundclinic.consultation.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import kotlinx.coroutines.flow.Flow

/** Opaque pagination cursor for [ConsultationMessageRepository.getByThread] — the (createdAt, id) of the oldest message loaded so far. */
data class MessageCursor(val createdAt: String, val id: String)

interface ConsultationMessageRepository {
    suspend fun save(entity: ConsultationMessageEntity): Resource<ConsultationMessageEntity>
    suspend fun getByConsultationId(consultationId: String, page: Int, size: Int): Resource<Pair<List<ConsultationMessageEntity>, Long>>

    /** Merged, cursor-paginated history across all consultations for a (doctorId, patientId) pair, newest first. */
    suspend fun getByThread(doctorId: String, patientId: String, before: MessageCursor?, size: Int): Resource<List<ConsultationMessageEntity>>
    fun watchMessages(consultationId: String): Flow<ConsultationMessageEntity>
    suspend fun getUserName(userId: String): String?

    /** Display name + profile picture URL, for rendering a conversation thread's counterpart. */
    suspend fun getUserInfo(userId: String): Pair<String, String?>?

    /** The user's persisted last-seen timestamp (set when their presence socket disconnects), null if never recorded. */
    suspend fun getLastSeenAt(userId: String): String?

    /** Most recent message for a (doctorId, patientId) pair, across all their consultations — used for the thread-list preview. */
    suspend fun getLatestForThread(doctorId: String, patientId: String): ConsultationMessageEntity?

    /** Count of messages still missing doctorId/patientId (pre-migration data) — used by the backfill task to skip once caught up. */
    suspend fun countMissingThreadFields(): Long

    /** Sets doctorId/patientId on messages of [consultationId] that don't have them yet. Returns the number of documents updated. Idempotent. */
    suspend fun backfillThreadFields(consultationId: String, doctorId: String, patientId: String): Long
}
