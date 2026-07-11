package ke.co.smartroundclinic.payments.domain.usecase.refund

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateChargebackRequestReq
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.RefundRepository
import org.slf4j.LoggerFactory

/**
 * Processes a single pending refund via IntaSend's native chargeback API — reverses the
 * original collection transaction by invoiceId instead of initiating a fresh outbound
 * disbursement, avoiding B2C-style payout charges. Invoked one refund at a time by
 * [ProcessRefundsTask].
 */
class ProcessNextRefundUseCase(
    private val refundRepository: RefundRepository,
    private val paymentRepository: PaymentRepository,
    private val intaSendRepository: IntaSendRepository,
) {
    private val log = LoggerFactory.getLogger(ProcessNextRefundUseCase::class.java)

    /** Returns true if a refund was picked up and attempted (regardless of outcome). */
    suspend operator fun invoke(): Boolean {
        val refund = (refundRepository.getNextPending() as? Resource.Success)?.data ?: return false

        val payment = (paymentRepository.getById(refund.paymentId) as? Resource.Success)?.data
        val invoiceId = payment?.invoiceId
        if (invoiceId.isNullOrBlank()) {
            log.warn("Refund id=${refund.id} — no invoiceId on file for paymentId=${refund.paymentId}, marking FAILED")
            refundRepository.markFailed(refund.id, "No invoice reference on file for the original payment")
            return true
        }

        val result = intaSendRepository.createChargeback(
            CreateChargebackRequestReq(
                invoiceId = invoiceId,
                amount = "%.2f".format(refund.amount),
                reason = refund.reason ?: "Appointment cancelled",
            )
        )

        if (result !is Resource.Success || result.data?.chargebackId == null) {
            val reason = (result as? Resource.Error)?.message ?: "Failed to create chargeback with payment provider"
            log.warn("Refund id=${refund.id} — chargeback creation failed: $reason")
            refundRepository.markFailed(refund.id, reason)
            return true
        }

        val chargebackId = result.data!!.chargebackId!!
        log.info("Refund id=${refund.id} submitted — chargebackId=$chargebackId invoiceId=$invoiceId amount=${refund.amount}")
        refundRepository.markSubmitted(refund.id, chargebackId)
        return true
    }
}
