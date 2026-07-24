package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.domain.repository.EarningsLedgerRepository
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionOverview
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentOverview
import ke.co.smartroundclinic.payments.presentation.dto.response.PlatformOverviewRes
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalOverview
import ke.co.smartroundclinic.payments.presentation.dto.response.toRes

/**
 * Platform-wide revenue snapshot. `payments`/`withdrawals`/`commission` totals are drawn from the
 * earnings ledger — a real record of confirmed IntaSend transfers, not a re-derived estimate — and
 * paired with live IntaSend wallet balances so any drift between "what our records say we earned"
 * and "what's actually sitting in IntaSend" is visible at a glance instead of silently wrong.
 */
class GetPlatformOverviewUseCase(
    private val paymentRepository: PaymentRepository,
    private val withdrawalRepository: WithdrawalRepository,
    private val earningsLedgerRepository: EarningsLedgerRepository,
    private val intaSendRepository: IntaSendRepository,
    private val config: IntaSendConfig,
) {
    suspend operator fun invoke(): DefaultResponse<PlatformOverviewRes?> {
        val payments = (paymentRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()
        val withdrawals = (withdrawalRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()
        val ledger = (earningsLedgerRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()

        val completed = payments.filter { it.status == PaymentEntity.PaymentStatus.COMPLETED }
        val pending = payments.filter { it.status == PaymentEntity.PaymentStatus.PENDING }
        val failed = payments.filter { it.status == PaymentEntity.PaymentStatus.FAILED }
        val totalGross = completed.sumOf { it.amount }

        val totalCommissionEarned = ledger.filter { it.commissionCreditedAt != null }.sumOf { it.commissionAmount }
        val totalNetToDoctor = ledger.filter { it.doctorCreditedAt != null }.sumOf { it.netAmount }

        val pendingW = withdrawals.filter { it.status == WithdrawalEntity.WithdrawalStatus.PENDING.name }
        val completedW = withdrawals.filter { it.status == WithdrawalEntity.WithdrawalStatus.COMPLETED.name }
        val failedW = withdrawals.filter { it.status == WithdrawalEntity.WithdrawalStatus.FAILED.name }

        val collectionsWalletBalance = (intaSendRepository.getWallet(config.collectionsWalletId) as? Resource.Success)?.data?.toRes()
        val commissionWalletBalance = (intaSendRepository.getWallet(config.commissionWalletId) as? Resource.Success)?.data?.toRes()

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Platform overview fetched successfully",
            data = PlatformOverviewRes(
                payments = PaymentOverview(
                    total = payments.size,
                    completedCount = completed.size,
                    pendingCount = pending.size,
                    failedCount = failed.size,
                    totalGross = totalGross,
                    totalCompletedAmount = totalGross,
                    totalPendingAmount = pending.sumOf { it.amount },
                    totalCommissionEarned = totalCommissionEarned,
                    totalNetToDoctor = totalNetToDoctor,
                    uniqueDoctors = payments.map { it.doctorId }.distinct().size,
                ),
                withdrawals = WithdrawalOverview(
                    total = withdrawals.size,
                    pendingCount = pendingW.size,
                    completedCount = completedW.size,
                    failedCount = failedW.size,
                    pendingAmount = pendingW.sumOf { it.amount },
                    completedAmount = completedW.sumOf { it.amount },
                    totalDisbursed = completedW.sumOf { it.amount },
                    uniqueDoctors = withdrawals.map { it.doctorId }.distinct().size,
                ),
                commission = CommissionOverview(
                    totalEarned = totalCommissionEarned,
                    entriesCount = ledger.size,
                ),
                collectionsWalletBalance = collectionsWalletBalance,
                commissionWalletBalance = commissionWalletBalance,
            )
        )
    }
}
