package ke.co.smartroundclinic.payments.domain.usecase.admin

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.PlatformOverviewRes

/**
 * Platform-wide revenue snapshot, deliberately kept to four numbers:
 * - `transactionsProcessed` / `collected` — cumulative, from confirmed local payment records
 *   (accurate all-time, unaffected by the earnings ledger's forward-only nature).
 * - `currentCommissionRate` — the live platform-wide rate (`admin_commission_rates`), not a
 *   historical snapshot off any one payment.
 * - `commission` — the live IntaSend commission wallet balance, since nothing in this app moves
 *   money out of that wallet, it's an always-accurate running total with no reconciliation needed.
 */
class GetPlatformOverviewUseCase(
    private val paymentRepository: PaymentRepository,
    private val appointmentInfoLookup: AppointmentInfoLookup,
    private val intaSendRepository: IntaSendRepository,
    private val config: IntaSendConfig,
) {
    suspend operator fun invoke(): DefaultResponse<PlatformOverviewRes?> {
        val payments = (paymentRepository.getAllForAdmin() as? Resource.Success)?.data ?: emptyList()
        val completed = payments.filter { it.status == PaymentEntity.PaymentStatus.COMPLETED }

        val commissionWallet = (intaSendRepository.getWallet(config.commissionWalletId) as? Resource.Success)?.data

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Platform overview fetched successfully",
            data = PlatformOverviewRes(
                transactionsProcessed = completed.size,
                currentCommissionRate = appointmentInfoLookup.getCommissionRate(),
                commission = commissionWallet?.currentBalance ?: 0.0,
                collected = completed.sumOf { it.amount },
            )
        )
    }
}
