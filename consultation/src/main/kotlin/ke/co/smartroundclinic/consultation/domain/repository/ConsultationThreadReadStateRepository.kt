package ke.co.smartroundclinic.consultation.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationThreadReadStateEntity

interface ConsultationThreadReadStateRepository {
    suspend fun getByPair(doctorId: String, patientId: String): Resource<ConsultationThreadReadStateEntity?>

    /** Batch fetch for the thread-list screen — one query for every (doctorId, patientId) pair returned. */
    suspend fun getByPairs(pairs: List<Pair<String, String>>): Resource<List<ConsultationThreadReadStateEntity>>

    /** Advances (never regresses) the reader's lastReadAt watermark for this thread. Upserts if absent. */
    suspend fun markRead(doctorId: String, patientId: String, readerId: String, at: String): Resource<ConsultationThreadReadStateEntity?>

    /** Advances (never regresses) the recipient's lastDeliveredAt watermark for this thread. Upserts if absent. */
    suspend fun bumpDelivered(doctorId: String, patientId: String, recipientId: String, at: String): Resource<ConsultationThreadReadStateEntity?>
}
