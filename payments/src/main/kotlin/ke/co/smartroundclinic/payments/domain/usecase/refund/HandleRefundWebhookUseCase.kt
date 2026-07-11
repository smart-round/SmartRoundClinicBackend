package ke.co.smartroundclinic.payments.domain.usecase.refund

import ke.co.smartroundclinic.payments.data.entity.RefundEntity
import ke.co.smartroundclinic.payments.data.remote.dto.response.WithdrawalWebhookPayload
import ke.co.smartroundclinic.payments.domain.repository.RefundRepository
import org.slf4j.LoggerFactory

/** Same disbursement webhook shape IntaSend sends for any send-money payout, keyed by trackingId. */
class HandleRefundWebhookUseCase(private val repository: RefundRepository) {

    private val log = LoggerFactory.getLogger(HandleRefundWebhookUseCase::class.java)

    suspend operator fun invoke(payload: WithdrawalWebhookPayload) {
        val trackingId = payload.trackingId ?: run {
            log.warn("RefundWebhook received with no trackingId — ignoring")
            return
        }

        log.info(
            "RefundWebhook trackingId=$trackingId status=${payload.status} " +
            "statusCode=${payload.statusCode} totalAmount=${payload.totalAmount} " +
            "paidAmount=${payload.paidAmount} failedAmount=${payload.failedAmount}"
        )

        val newStatus = when (payload.statusCode) {
            "BP200" -> RefundEntity.RefundStatus.COMPLETED.name
            "BP400" -> RefundEntity.RefundStatus.FAILED.name
            else -> {
                log.info("RefundWebhook trackingId=$trackingId statusCode=${payload.statusCode} — in progress, no status change")
                return
            }
        }

        repository.updateStatusByTrackingId(trackingId, newStatus)
    }
}
