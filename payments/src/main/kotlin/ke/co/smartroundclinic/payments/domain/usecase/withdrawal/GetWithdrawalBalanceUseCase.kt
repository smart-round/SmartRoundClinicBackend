package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawalBalanceRes

/**
 * `availableBalance` is read live from the doctor's IntaSend wallet — the authoritative number
 * for whether a withdrawal is actually allowed. The other fields are historical/informational,
 * computed from our own DB, and are not used to gate withdrawals.
 */
class GetWithdrawalBalanceUseCase(
    private val paymentRepository: PaymentRepository,
    private val withdrawalRepository: WithdrawalRepository,
    private val intaSendRepository: IntaSendRepository,
    private val walletResolver: DoctorWalletResolver,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<WithdrawalBalanceRes?> {
        val walletId = walletResolver.resolve(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Unable to reach your wallet right now. Please try again shortly.",
                data = null,
            )
        val walletResult = intaSendRepository.getWallet(walletId)
        val wallet = (walletResult as? Resource.Success)?.data
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = (walletResult as? Resource.Error)?.message ?: "Failed to fetch wallet balance",
                data = null,
            )
        val availableBalance = wallet.availableBalance
        val currentBalance = wallet.currentBalance

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
                availableBalance = availableBalance,
                currentBalance = currentBalance,
            )
        )
    }
}
