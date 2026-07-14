package ke.co.smartroundclinic.payments.domain.usecase.refund

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.RefundProcessor
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
 * avoiding B2C-style payout charges. Triggered exactly once, immediately, by
 * CancelAppointmentUseCase right after the refund record is created — there is no background
 * poller, so the chargeback call gets a short inline retry for transient failures.
 */
class ProcessRefundUseCase(
    private val refundRepository: RefundRepository,
    private val paymentRepository: PaymentRepository,
    private val intaSendRepository: IntaSendRepository,
) : RefundProcessor {
    private val log = LoggerFactory.getLogger(ProcessRefundUseCase::class.java)

    override suspend fun processRefund(refundId: String) {
        val refund = (refundRepository.getById(refundId) as? Resource.Success)?.data
        if (refund == null || refund.status != RefundEntity.RefundStatus.PENDING.name) {
            log.info("processRefund id=$refundId — not found or not pending, skipping")
            return
        }

        val payment = (paymentRepository.getById(refund.paymentId) as? Resource.Success)?.data
        val invoiceId = payment?.invoiceId
        if (invoiceId.isNullOrBlank()) {
            log.warn("Refund id=$refundId — no invoiceId on file for paymentId=${refund.paymentId}, marking FAILED")
            refundRepository.markFailed(refundId, "No invoice reference on file for the original payment")
            return
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
            return
        }

        val chargebackId = result.data!!.chargebackId!!
        log.info("Refund id=$refundId submitted — chargebackId=$chargebackId invoiceId=$invoiceId amount=${refund.amount}")
        refundRepository.markSubmitted(refundId, chargebackId)
    }
}
