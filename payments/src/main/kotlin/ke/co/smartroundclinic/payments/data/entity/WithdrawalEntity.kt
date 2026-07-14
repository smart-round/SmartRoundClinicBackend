package ke.co.smartroundclinic.payments.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class WithdrawalTransactionRecord(
    val account: String,
    val amount: String,
    val bankCode: String,
    val name: String,
)

data class WithdrawalEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val amount: Double,
    val currency: String = "KES",
    val trackingId: String,
    val status: String = WithdrawalStatus.PENDING.name,
    val provider: String,
    val platformCommission: Double,
    val transactions: List<WithdrawalTransactionRecord>,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
    // Populated from the IntaSend webhook once the disbursement settles — kept separate from
    // the request-time fields above so the audit trail shows what was actually charged/paid,
    // not just what was requested.
    val statusCode: String? = null,
    val statusDescription: String? = null,
    val actualCharge: String? = null,
    val paidAmount: String? = null,
    val providerReference: String? = null,
) {
    enum class WithdrawalStatus { PENDING, COMPLETED, FAILED }
}
