package ke.co.smartroundclinic.payments.presentation.dto.response

import kotlinx.serialization.Serializable

// ── Platform overview ─────────────────────────────────────────────────────────

@Serializable
data class PlatformOverviewRes(
    /** Count of successfully completed payments, all-time. */
    val transactionsProcessed: Int,
    /** The live platform-wide commission rate (percent), from admin_commission_rates. */
    val currentCommissionRate: Double,
    /** Live IntaSend commission wallet balance — the platform's running commission total. */
    val commission: Double,
    /** Cumulative gross amount collected from patients, all-time (sum of completed payments). */
    val collected: Double,
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

// ── Earnings chart — revenue vs commission over time, for a line/bar chart ────

@Serializable
data class EarningsChartRes(
    /** "day" | "week" | "month" | "year" — echoes the requested range. */
    val range: String,
    val from: String,
    val to: String,
    val points: List<EarningsDataPoint>,
    val totals: CommissionPeriodStats,
)

@Serializable
data class EarningsDataPoint(
    /** Bucket key: hourly ("yyyy-MM-ddTHH:00") for range=day, daily for week/month, monthly for year. */
    val label: String,
    val commission: Double,
    val revenue: Double,
    val disbursed: Double,
    val transactionCount: Int,
)

// ── Revenue breakdown — composition of confirmed revenue, for a pie/donut chart ─

@Serializable
data class RevenueBreakdownRes(
    /** "day" | "week" | "month" | "year" — echoes the requested range. */
    val range: String,
    val collected: Double,
    val commission: Double,
    val netToDoctor: Double,
    /** Collected with commission confirmed but the doctor's leg not yet credited (rare — a leg lagging or retrying). */
    val doctorPayoutPending: Double,
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
    val totalRevenue: Double,
    val totalDisbursed: Double,
    val transactionCount: Int,
)

// ── Commission audit log list ─────────────────────────────────────────────────

@Serializable
data class CommissionLogItemRes(
    val id: String,
    val paymentId: String,
    val appointmentId: String?,
    val doctorId: String,
    val grossAmount: Double,
    val commissionRate: Double,
    val commissionAmount: Double,
    val netAmount: Double,
    val doctorCreditedAt: String?,
    val commissionCreditedAt: String?,
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
