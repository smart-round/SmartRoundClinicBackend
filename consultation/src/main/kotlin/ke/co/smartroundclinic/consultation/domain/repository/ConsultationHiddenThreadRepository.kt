package ke.co.smartroundclinic.consultation.domain.repository

import ke.co.smartroundclinic.common.Resource

interface ConsultationHiddenThreadRepository {
    /** Upserts hiddenAt=now for (userId, doctorId, patientId). */
    suspend fun hide(userId: String, doctorId: String, patientId: String): Resource<Unit>

    /** One batch query — hiddenAt per (doctorId, patientId) pair, for the caller's own hidden threads. */
    suspend fun getHiddenMap(userId: String): Resource<Map<Pair<String, String>, String>>
}
