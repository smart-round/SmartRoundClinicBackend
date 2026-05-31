package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalBalanceRes

class GetWithdrawalBalanceUseCase(
    private val paymentRepository: PaymentRepository,
    private val withdrawalRepository: WithdrawalRepository,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<WithdrawalBalanceRes?> {
        val payments = (paymentRepository.getAllByDoctorId(doctorId) as? Resource.Success)?.data ?: emptyList()
        val totalNetEarnings = payments
            .filter { it.status == PaymentEntity.PaymentStatus.COMPLETED }
            .sumOf { it.amount * (1.0 - it.commissionRate / 100.0) }

        val withdrawals = (withdrawalRepository.getByDoctorId(doctorId) as? Resource.Success)?.data ?: emptyList()

        val totalPending = withdrawals
            .filter { it.status == WithdrawalEntity.WithdrawalStatus.PENDING.name }
            .sumOf { it.amount }

        val totalCompleted = withdrawals
            .filter { it.status == WithdrawalEntity.WithdrawalStatus.COMPLETED.name }
            .sumOf { it.amount }

        // Both PENDING and COMPLETED count against the available balance.
        // PENDING = disbursement in-flight; funds are already committed.
        val totalWithdrawn = totalPending + totalCompleted

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Withdrawal balance fetched successfully",
            data = WithdrawalBalanceRes(
                totalNetEarnings = totalNetEarnings,
                totalWithdrawn = totalWithdrawn,
                totalPending = totalPending,
                totalCompleted = totalCompleted,
                availableBalance = totalNetEarnings - totalWithdrawn,
            )
        )
    }
}
