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
) {
    enum class WithdrawalStatus { PENDING, COMPLETED, FAILED }
}
