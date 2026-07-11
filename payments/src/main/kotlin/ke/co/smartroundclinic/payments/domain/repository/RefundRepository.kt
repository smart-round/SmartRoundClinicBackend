package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.RefundEntity

interface RefundRepository {
    /** Oldest refund with status=PENDING and no trackingId yet (i.e. never attempted). */
    suspend fun getNextPending(): Resource<RefundEntity?>
    suspend fun getById(id: String): Resource<RefundEntity?>

    /** Marks a refund as submitted to the payment provider — stays PENDING until the webhook confirms it. */
    suspend fun markSubmitted(id: String, trackingId: String): Resource<Unit>
    suspend fun markFailed(id: String, reason: String): Resource<Unit>
    suspend fun updateStatusByTrackingId(trackingId: String, status: String): Resource<Unit>
}
