package ke.co.smartroundclinic.payments.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

/** Which side of a completed appointment's payment split this credit confirms. */
enum class EarningsLedgerLeg { DOCTOR, COMMISSION }

/**
 * One row per payment, upserted by [CreditDoctorEarningsUseCase] the moment each leg's IntaSend
 * wallet transfer actually succeeds — never written on a failed/pending leg. This is a real ledger
 * of confirmed money movement, not a re-derived estimate from payment status.
 */
data class EarningsLedgerEntity(
    val id: String = ObjectId().toString(),
    val paymentId: String,
    val appointmentId: String?,
    val doctorId: String,
    val grossAmount: Double,
    val commissionRate: Double,
    val commissionAmount: Double,
    val netAmount: Double,
    val doctorCreditedAt: String? = null,
    val commissionCreditedAt: String? = null,
    val createdAt: String = Clock.System.now().toString(),
)
