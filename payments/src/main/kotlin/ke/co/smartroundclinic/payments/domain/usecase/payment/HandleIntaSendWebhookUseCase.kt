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
        val invoiceId = payload.invoiceId ?: return
        val state = payload.state?.uppercase() ?: return

        log.info(
            "IntaSend webhook — invoiceId=$invoiceId state=$state provider=${payload.provider} " +
            "value=${payload.value} currency=${payload.currency} account=${payload.account} " +
            "mpesaRef=${payload.mpesaReference} apiRef=${payload.apiRef}"
        )

        when (state) {
            "COMPLETE" -> handleComplete(payload, invoiceId)
            "PENDING"  -> log.info("IntaSend invoiceId=$invoiceId PENDING — waiting for payment confirmation")
            "PROCESSING" -> log.info("IntaSend invoiceId=$invoiceId PROCESSING — payment in progress")
            "FAILED"   -> log.warn(
                "IntaSend invoiceId=$invoiceId FAILED — reason=${payload.failedReason} " +
                "code=${payload.failedCode}"
            )
            else -> log.info("IntaSend invoiceId=$invoiceId unhandled state=$state")
        }
    }

    private suspend fun handleComplete(payload: IntaSendCallbackPayload, invoiceId: String) {
        // api_ref = "ISL_<paymentLinkUUID>" — strip the prefix to get our transactionRef
        val transactionRef = payload.apiRef?.removePrefix("ISL_")
        if (transactionRef.isNullOrBlank()) {
            log.warn("COMPLETE invoiceId=$invoiceId — api_ref missing, cannot link to payment record")
            return
        }

        when (val result = paymentRepository.getByTransactionRef(transactionRef)) {
            is Resource.Success -> {
                val existing = result.data ?: run {
                    log.warn("COMPLETE invoiceId=$invoiceId — no payment record for transactionRef=$transactionRef")
                    return
                }
                paymentRepository.updateFromWebhook(
                    id = existing.id,
                    status = PaymentEntity.PaymentStatus.COMPLETED.name,
                    invoiceId = invoiceId,
                    mpesaReference = payload.mpesaReference,
                    charges = payload.charges,
                    netAmount = payload.netAmount,
                    account = payload.account,
                    paymentMethod = payload.provider,
                )
                log.info(
                    "Payment COMPLETED — id=${existing.id} appointmentId=${existing.appointmentId} " +
                    "invoiceId=$invoiceId mpesaRef=${payload.mpesaReference} " +
                    "value=${payload.value} netAmount=${payload.netAmount}"
                )
            }
            is Resource.Error -> log.error("Failed to fetch payment for transactionRef=$transactionRef — ${result.message}")
        }
    }
}
