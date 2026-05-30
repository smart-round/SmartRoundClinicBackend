package ke.co.smartroundclinic.payments.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class DoctorPaymentSummaryRes(
    val totalEarned: Double,
    val totalPending: Double,
    val completedCount: Int,
    val pendingCount: Int,
    val totalTransactions: Int,
)
