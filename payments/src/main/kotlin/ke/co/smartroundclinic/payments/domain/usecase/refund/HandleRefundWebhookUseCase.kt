package ke.co.smartroundclinic.payments.domain.usecase.refund

import ke.co.smartroundclinic.payments.data.entity.RefundEntity
import ke.co.smartroundclinic.payments.data.remote.dto.response.ChargebackWebhookPayload
import ke.co.smartroundclinic.payments.domain.repository.RefundRepository
import org.slf4j.LoggerFactory

/** IntaSend fires this whenever a chargeback's status changes, keyed by chargebackId. */
class HandleRefundWebhookUseCase(private val repository: RefundRepository) {

    private val log = LoggerFactory.getLogger(HandleRefundWebhookUseCase::class.java)

    suspend operator fun invoke(payload: ChargebackWebhookPayload) {
        val chargebackId = payload.chargebackId ?: run {
            log.warn("RefundWebhook received with no chargebackId — ignoring")
            return
        }

        log.info("RefundWebhook chargebackId=$chargebackId status=${payload.status} amount=${payload.amount}")

        val newStatus = when (payload.status?.uppercase()) {
            "COMPLETED" -> RefundEntity.RefundStatus.COMPLETED.name
            "CANCELLED", "OVERDUE" -> RefundEntity.RefundStatus.FAILED.name
            else -> {
                log.info("RefundWebhook chargebackId=$chargebackId status=${payload.status} — in progress, no status change")
                return
            }
        }

        repository.updateStatusByTrackingId(chargebackId, newStatus)
    }
}
