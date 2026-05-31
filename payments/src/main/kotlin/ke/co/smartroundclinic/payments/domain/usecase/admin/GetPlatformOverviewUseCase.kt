package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.PlatformCommissionLogRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.CommissionOverview
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentOverview
import ke.co.smartroundclinic.payments.presentation.dto.response.PlatformOverviewRes
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalOverview

class GetPlatformOverviewUseCase(
    private val paymentRepository: PaymentRepository,
    private val withdrawalRepository: WithdrawalRepository,
    private val commissionLogRepository: PlatformCommissionLogRepository,
) {
    suspend operator fun invoke(): DefaultResponse<PlatformOverviewRes?> {
        val payments = (paymentRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()
        val withdrawals = (withdrawalRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()
        val commissionLogs = (commissionLogRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()

        val completed = payments.filter { it.status == PaymentEntity.PaymentStatus.COMPLETED }
        val pending = payments.filter { it.status == PaymentEntity.PaymentStatus.PENDING }
        val failed = payments.filter { it.status == PaymentEntity.PaymentStatus.FAILED }
        val totalGross = completed.sumOf { it.amount }
        val totalCommission = completed.sumOf { it.amount * (it.commissionRate / 100.0) }

        val pendingW = withdrawals.filter { it.status == WithdrawalEntity.WithdrawalStatus.PENDING.name }
        val completedW = withdrawals.filter { it.status == WithdrawalEntity.WithdrawalStatus.COMPLETED.name }
        val failedW = withdrawals.filter { it.status == WithdrawalEntity.WithdrawalStatus.FAILED.name }

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
                    totalCommissionEarned = totalCommission,
                    totalNetToDoctor = totalGross - totalCommission,
                    uniqueDoctors = payments.map { it.doctorId }.distinct().size,
                ),
                withdrawals = WithdrawalOverview(
                    total = withdrawals.size,
                    pendingCount = pendingW.size,
                    completedCount = completedW.size,
                    failedCount = failedW.size,
                    pendingAmount = pendingW.sumOf { it.amount },
                    completedAmount = completedW.sumOf { it.amount },
                    totalDisbursed = (pendingW + completedW).sumOf { it.amount },
                    uniqueDoctors = withdrawals.map { it.doctorId }.distinct().size,
                ),
                commission = CommissionOverview(
                    totalEarned = totalCommission,
                    totalAuditLogs = commissionLogs.size,
                ),
            )
        )
    }
}
