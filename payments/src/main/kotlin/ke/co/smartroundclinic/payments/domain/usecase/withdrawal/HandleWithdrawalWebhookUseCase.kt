package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.data.remote.dto.response.WithdrawalWebhookPayload
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import org.slf4j.LoggerFactory

class HandleWithdrawalWebhookUseCase(private val repository: WithdrawalRepository) {

    private val log = LoggerFactory.getLogger(HandleWithdrawalWebhookUseCase::class.java)

    suspend operator fun invoke(payload: WithdrawalWebhookPayload) {
        val trackingId = payload.trackingId ?: run {
            log.warn("WithdrawalWebhook received with no trackingId — ignoring")
            return
        }

        log.info(
            "WithdrawalWebhook trackingId=$trackingId status=${payload.status} " +
            "statusCode=${payload.statusCode} totalAmount=${payload.totalAmount} " +
            "paidAmount=${payload.paidAmount} failedAmount=${payload.failedAmount}"
        )

        val newStatus = when (payload.statusCode) {
            "BP200" -> WithdrawalEntity.WithdrawalStatus.COMPLETED.name
            "BP400" -> WithdrawalEntity.WithdrawalStatus.FAILED.name
            else -> {
                log.info("WithdrawalWebhook trackingId=$trackingId statusCode=${payload.statusCode} — in progress, no status change")
                return
            }
        }

        repository.updateStatus(trackingId, newStatus)
    }
}
