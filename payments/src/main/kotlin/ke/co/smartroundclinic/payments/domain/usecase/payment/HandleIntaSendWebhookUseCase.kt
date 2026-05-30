package ke.co.smartroundclinic.payments.domain.usecase.payment

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory

class HandleIntaSendWebhookUseCase(
    private val paymentRepository: PaymentRepository,
) {
    private val log = LoggerFactory.getLogger(HandleIntaSendWebhookUseCase::class.java)

    suspend operator fun invoke(payload: IntaSendCallbackPayload) {
        val state = payload.state?.uppercase() ?: return
        val invoiceId = payload.invoiceId ?: return

        log.info(
            "IntaSend webhook — invoiceId=$invoiceId state=$state provider=${payload.provider} " +
            "value=${payload.value} currency=${payload.currency} account=${payload.account} " +
            "mpesaRef=${payload.mpesaReference} apiRef=${payload.apiRef}"
        )

        when (state) {
            "COMPLETE" -> handleComplete(payload, invoiceId)
            "FAILED" -> handleFailed(payload, invoiceId)
            else -> log.info("IntaSend webhook state=$state — no action taken")
        }
    }

    private suspend fun handleComplete(payload: IntaSendCallbackPayload, invoiceId: String) {
        val appointmentId = payload.apiRef
        if (appointmentId == null) {
            log.warn("COMPLETE webhook for invoiceId=$invoiceId has no api_ref — cannot link to appointment")
            return
        }

        when (val result = paymentRepository.getByAppointmentId(appointmentId)) {
            is Resource.Success -> {
                val existing = result.data
                if (existing == null) {
                    log.warn("COMPLETE webhook invoiceId=$invoiceId — no payment record for appointmentId=$appointmentId")
                    return
                }
                paymentRepository.updateStatus(
                    id = existing.id,
                    status = PaymentEntity.PaymentStatus.COMPLETED.name,
                    transactionRef = payload.mpesaReference ?: invoiceId,
                )
                log.info("Payment COMPLETED for appointmentId=$appointmentId invoiceId=$invoiceId mpesaRef=${payload.mpesaReference}")
            }
            is Resource.Error -> log.error("Failed to fetch payment for appointmentId=$appointmentId — ${result.message}")
        }
    }

    private fun handleFailed(payload: IntaSendCallbackPayload, invoiceId: String) {
        log.warn(
            "Payment FAILED — invoiceId=$invoiceId reason=${payload.failedReason} " +
            "code=${payload.failedCode} apiRef=${payload.apiRef}"
        )
    }
}
