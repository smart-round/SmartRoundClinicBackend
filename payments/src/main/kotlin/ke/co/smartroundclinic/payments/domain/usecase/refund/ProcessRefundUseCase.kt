package ke.co.smartroundclinic.payments.domain.usecase.refund

import ke.co.smartroundclinic.common.RefundProcessResult
import ke.co.smartroundclinic.common.RefundProcessor
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.RefundEntity
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateChargebackRequestReq
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.RefundRepository
import ke.co.smartroundclinic.payments.domain.usecase.retryResource
import org.slf4j.LoggerFactory

/**
 * Processes one specific refund via IntaSend's native chargeback API — reverses the original
 * collection transaction by invoiceId instead of initiating a fresh outbound disbursement,
 * avoiding B2C-style payout charges. Admin-triggered only (see AdminRefundController's process
 * endpoint in :scheduling, which calls this via the RefundProcessor cross-module interface) —
 * there is no automatic trigger on cancellation and no background poller.
 */
class ProcessRefundUseCase(
    private val refundRepository: RefundRepository,
    private val paymentRepository: PaymentRepository,
    private val intaSendRepository: IntaSendRepository,
    private val config: IntaSendConfig,
) : RefundProcessor {
    private val log = LoggerFactory.getLogger(ProcessRefundUseCase::class.java)

    override suspend fun processRefund(refundId: String): RefundProcessResult {
        val refund = (refundRepository.getById(refundId) as? Resource.Success)?.data
        if (refund == null) {
            log.info("processRefund id=$refundId — not found")
            return RefundProcessResult.NotFound
        }
        if (refund.status != RefundEntity.RefundStatus.PENDING.name) {
            log.info("processRefund id=$refundId — status=${refund.status}, not PENDING, skipping")
            return RefundProcessResult.NotPending
        }

        val payment = (paymentRepository.getById(refund.paymentId) as? Resource.Success)?.data
        val invoiceId = payment?.invoiceId
        if (invoiceId.isNullOrBlank()) {
            val reason = "No invoice reference on file for the original payment"
            log.warn("Refund id=$refundId — $reason, marking FAILED")
            refundRepository.markFailed(refundId, reason)
            return RefundProcessResult.Failed(reason)
        }

        // Refunds are funded out of the collections wallet's AVAILABLE balance, which can be lower
        // than its current balance (funds held in reserve aren't spendable) — check before ever
        // attempting the chargeback, since IntaSend would otherwise reject it anyway.
        val walletResult = intaSendRepository.getWallet(config.collectionsWalletId)
        val wallet = (walletResult as? Resource.Success)?.data
        if (wallet == null) {
            val reason = (walletResult as? Resource.Error)?.message ?: "Failed to check collections wallet balance"
            log.error("Refund id=$refundId — $reason")
            return RefundProcessResult.Failed(reason)
        }
        if (wallet.availableBalance < refund.amount) {
            log.warn(
                "Refund id=$refundId amount=${refund.amount} — collections wallet available " +
                "balance ${wallet.availableBalance} is insufficient, leaving PENDING"
            )
            return RefundProcessResult.InsufficientBalance(
                requiredAmount = refund.amount,
                availableBalance = wallet.availableBalance,
            )
        }

        val result = retryResource {
            intaSendRepository.createChargeback(
                CreateChargebackRequestReq(
                    invoiceId = invoiceId,
                    amount = "%.2f".format(refund.amount),
                    reason = refund.reason ?: "Appointment cancelled",
                )
            )
        }

        if (result !is Resource.Success || result.data?.chargebackId == null) {
            val reason = (result as? Resource.Error)?.message ?: "Failed to create chargeback with payment provider"
            log.warn("Refund id=$refundId — chargeback creation failed after retries: $reason")
            refundRepository.markFailed(refundId, reason)
            return RefundProcessResult.Failed(reason)
        }

        val chargebackId = result.data!!.chargebackId!!
        log.info("Refund id=$refundId submitted — chargebackId=$chargebackId invoiceId=$invoiceId amount=${refund.amount}")
        refundRepository.markSubmitted(refundId, chargebackId)
        return RefundProcessResult.Submitted(chargebackId)
    }
}
