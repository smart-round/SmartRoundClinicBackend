package ke.co.smartroundclinic.payments.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class CommissionPaymentEntry(
    val paymentId: String,
    val appointmentId: String,
    val doctorId: String,
    val amount: Double,
    val commissionRate: Double,
    val commissionAmount: Double,
    val paidAt: String,
)

data class PlatformCommissionLogEntity(
    val id: String = ObjectId().toString(),
    val withdrawalId: String,
    val doctorId: String,
    val withdrawalAmount: Double,
    val totalGross: Double,
    val totalCommission: Double,
    val payments: List<CommissionPaymentEntry>,
    val createdAt: String = Clock.System.now().toString(),
)
