package ke.co.smartroundclinic.payments.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

/**
 * Mirrors the document shape written by :scheduling's RefundLookup into the shared
 * `refunds` collection on appointment cancellation. Owned here for payout processing since
 * :payments already has typed access to the payments database.
 */
data class RefundEntity(
    val id: String = ObjectId().toString(),
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val paymentId: String,
    val amount: Double,
    val currency: String = "KES",
    val reason: String? = null,
    val cancelledBy: String,
    val cancelledByRole: String,
    val status: String = RefundStatus.PENDING.name,
    /** Holds IntaSend's chargeback_id once the refund is submitted. */
    val trackingId: String? = null,
    val failureReason: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    enum class RefundStatus { PENDING, COMPLETED, FAILED }
}
