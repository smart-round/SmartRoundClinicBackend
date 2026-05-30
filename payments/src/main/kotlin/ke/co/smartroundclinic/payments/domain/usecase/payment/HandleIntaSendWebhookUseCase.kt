package ke.co.smartroundclinic.payments.domain.usecase.payment

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.entity.PaymentLogEntity
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.domain.repository.PaymentLogRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory

class HandleIntaSendWebhookUseCase(
    private val paymentRepository: PaymentRepository,
    private val paymentLogRepository: PaymentLogRepository,
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

        val linkedPaymentEntityId: String?
        val linkedAppointmentId: String?

        when (state) {
            "COMPLETE" -> {
                val (entityId, appointmentId) = handleComplete(payload, invoiceId)
                linkedPaymentEntityId = entityId
                linkedAppointmentId = appointmentId
            }
            "PENDING" -> {
                log.info("IntaSend invoiceId=$invoiceId PENDING — waiting for payment confirmation")
                linkedPaymentEntityId = null
                linkedAppointmentId = null
            }
            "PROCESSING" -> {
                log.info("IntaSend invoiceId=$invoiceId PROCESSING — payment in progress")
                linkedPaymentEntityId = null
                linkedAppointmentId = null
            }
            "FAILED" -> {
                log.warn("IntaSend invoiceId=$invoiceId FAILED — reason=${payload.failedReason} code=${payload.failedCode}")
                linkedPaymentEntityId = null
                linkedAppointmentId = null
            }
            else -> {
                log.info("IntaSend invoiceId=$invoiceId unhandled state=$state")
                linkedPaymentEntityId = null
                linkedAppointmentId = null
            }
        }

        runCatching {
            paymentLogRepository.log(
                PaymentLogEntity(
                    invoiceId = invoiceId,
                    state = state,
                    provider = payload.provider,
                    value = payload.value,
                    charges = payload.charges,
                    netAmount = payload.netAmount,
                    currency = payload.currency,
                    account = payload.account,
                    mpesaReference = payload.mpesaReference,
                    providerRef = payload.providerRef,
                    apiRef = payload.apiRef,
                    paymentEntityId = linkedPaymentEntityId,
                    appointmentId = linkedAppointmentId,
                )
            )
        }.onFailure { log.error("Failed to write payment log invoiceId=$invoiceId — ${it.message}", it) }
    }

    /** Returns (paymentEntityId, appointmentId) on success, (null, null) otherwise. */
    private suspend fun handleComplete(payload: IntaSendCallbackPayload, invoiceId: String): Pair<String?, String?> {
        val transactionRef = payload.apiRef?.removePrefix("ISL_")
        if (transactionRef.isNullOrBlank()) {
            log.warn("COMPLETE invoiceId=$invoiceId — api_ref missing, cannot link to payment record")
            return null to null
        }

        return when (val result = paymentRepository.getByTransactionRef(transactionRef)) {
            is Resource.Success -> {
                val existing = result.data ?: run {
                    log.warn("COMPLETE invoiceId=$invoiceId — no payment record for transactionRef=$transactionRef")
                    return null to null
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
                existing.id to existing.appointmentId
            }
            is Resource.Error -> {
                log.error("Failed to fetch payment for transactionRef=$transactionRef — ${result.message}")
                null to null
            }
        }
    }
}
