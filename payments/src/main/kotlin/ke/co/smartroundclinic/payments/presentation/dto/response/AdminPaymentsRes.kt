package ke.co.smartroundclinic.payments.presentation.dto.response

import kotlinx.serialization.Serializable

// ── Platform overview ─────────────────────────────────────────────────────────

@Serializable
data class PlatformOverviewRes(
    val payments: PaymentOverview,
    val withdrawals: WithdrawalOverview,
    val commission: CommissionOverview,
)

@Serializable
data class PaymentOverview(
    val total: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val failedCount: Int,
    val totalGross: Double,
    val totalCompletedAmount: Double,
    val totalPendingAmount: Double,
    val totalCommissionEarned: Double,
    val totalNetToDoctor: Double,
    val uniqueDoctors: Int,
)

@Serializable
data class WithdrawalOverview(
    val total: Int,
    val pendingCount: Int,
    val completedCount: Int,
    val failedCount: Int,
    val pendingAmount: Double,
    val completedAmount: Double,
    val totalDisbursed: Double,
    val uniqueDoctors: Int,
)

@Serializable
data class CommissionOverview(
    val totalEarned: Double,
    val totalAuditLogs: Int,
)

// ── Per-doctor breakdown ──────────────────────────────────────────────────────

@Serializable
data class DoctorPaymentBreakdownRes(
    val doctorId: String,
    val totalGross: Double,
    val totalCommission: Double,
    val totalNetEarnings: Double,
    val totalWithdrawn: Double,
    val pendingWithdrawals: Double,
    val completedWithdrawals: Double,
    val availableBalance: Double,
    val completedPaymentsCount: Int,
    val pendingPaymentsCount: Int,
    val totalWithdrawalsCount: Int,
)

@Serializable
data class DoctorPaymentBreakdownsRes(
    val doctors: List<DoctorPaymentBreakdownRes>,
    val total: Int,
)

// ── Withdrawals list ──────────────────────────────────────────────────────────

@Serializable
data class WithdrawalItemRes(
    val id: String,
    val doctorId: String,
    val amount: Double,
    val currency: String,
    val trackingId: String,
    val status: String,
    val provider: String,
    val platformCommission: Double,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class WithdrawalsPageRes(
    val items: List<WithdrawalItemRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

// ── Earnings chart ────────────────────────────────────────────────────────────

@Serializable
data class EarningsChartRes(
    val from: String,
    val to: String,
    val points: List<EarningsDataPoint>,
    val totals: CommissionPeriodStats,
)

@Serializable
data class EarningsDataPoint(
    val date: String,
    val commission: Double,
    val gross: Double,
    val disbursed: Double,
    val transactionCount: Int,
)

// ── Commission time summary ───────────────────────────────────────────────────

@Serializable
data class CommissionTimeSummaryRes(
    val total: CommissionPeriodStats,
    val monthly: CommissionPeriodStats,
    val weekly: CommissionPeriodStats,
    val daily: CommissionPeriodStats,
)

@Serializable
data class CommissionPeriodStats(
    val totalCommission: Double,
    val totalGross: Double,
    val totalDisbursed: Double,
    val transactionCount: Int,
)

// ── Commission audit log list ─────────────────────────────────────────────────

@Serializable
data class CommissionLogItemRes(
    val id: String,
    val withdrawalId: String,
    val doctorId: String,
    val withdrawalAmount: Double,
    val totalGross: Double,
    val totalCommission: Double,
    val paymentsCount: Int,
    val createdAt: String,
)

@Serializable
data class CommissionLogsPageRes(
    val items: List<CommissionLogItemRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)
