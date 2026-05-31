package ke.co.smartroundclinic.payments.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class DoctorPaymentSummaryRes(
    // ── Earnings ──────────────────────────────────────────────────────────────
    val totalGross: Double,
    val totalPlatformFees: Double,
    val totalNetEarnings: Double,
    val totalPendingPayments: Double,
    val completedCount: Int,
    val pendingCount: Int,
    val totalTransactions: Int,
    // ── Withdrawals ───────────────────────────────────────────────────────────
    val totalWithdrawn: Double,
    val totalPendingWithdrawals: Double,
    val totalCompletedWithdrawals: Double,
    val availableBalance: Double,
)
