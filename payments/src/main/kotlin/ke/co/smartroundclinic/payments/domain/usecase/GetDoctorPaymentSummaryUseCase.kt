package ke.co.smartroundclinic.payments.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.presentation.dto.response.DoctorPaymentSummaryRes

/**
 * `availableBalance` is read live from the doctor's IntaSend wallet — the authoritative number
 * for whether a withdrawal is actually allowed. The other fields are historical/informational,
 * computed from our own DB, and are not used to gate withdrawals.
 */
class GetDoctorPaymentSummaryUseCase(
    private val paymentRepository: PaymentRepository,
    private val withdrawalRepository: WithdrawalRepository,
    private val intaSendRepository: IntaSendRepository,
    private val walletResolver: DoctorWalletResolver,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<DoctorPaymentSummaryRes?> {
        val paymentsResult = paymentRepository.getAllByDoctorId(doctorId)
        if (paymentsResult is Resource.Error) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = paymentsResult.message ?: "Failed to fetch payment summary",
                data = null,
            )
        }

        val walletId = walletResolver.resolve(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Unable to reach your wallet right now. Please try again shortly.",
                data = null,
            )
        val walletResult = intaSendRepository.getWallet(walletId)
        val availableBalance = (walletResult as? Resource.Success)?.data?.availableBalance
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = (walletResult as? Resource.Error)?.message ?: "Failed to fetch wallet balance",
                data = null,
            )

        val payments = (paymentsResult as Resource.Success).data ?: emptyList()
        val completed = payments.filter { it.status == PaymentEntity.PaymentStatus.COMPLETED }
        val pending = payments.filter { it.status == PaymentEntity.PaymentStatus.PENDING }

        val totalGross = completed.sumOf { it.amount }
        val totalPlatformFees = completed.sumOf { it.amount * (it.commissionRate / 100.0) }
        val totalNetEarnings = totalGross - totalPlatformFees

        val withdrawals = (withdrawalRepository.getByDoctorId(doctorId) as? Resource.Success)?.data ?: emptyList()
        val totalPendingWithdrawals = withdrawals
            .filter { it.status == WithdrawalEntity.WithdrawalStatus.PENDING.name }
            .sumOf { it.amount }
        val totalCompletedWithdrawals = withdrawals
            .filter { it.status == WithdrawalEntity.WithdrawalStatus.COMPLETED.name }
            .sumOf { it.amount }
        val totalWithdrawn = totalPendingWithdrawals + totalCompletedWithdrawals

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Payment summary fetched successfully",
            data = DoctorPaymentSummaryRes(
                totalGross = totalGross,
                totalPlatformFees = totalPlatformFees,
                totalNetEarnings = totalNetEarnings,
                totalPendingPayments = pending.sumOf { it.amount },
                completedCount = completed.size,
                pendingCount = pending.size,
                totalTransactions = payments.size,
                totalWithdrawn = totalWithdrawn,
                totalPendingWithdrawals = totalPendingWithdrawals,
                totalCompletedWithdrawals = totalCompletedWithdrawals,
                availableBalance = availableBalance,
            )
        )
    }
}
