package ke.co.smartroundclinic.payments.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.remote.instasend.request.GetPaymentStatusReq
import ke.co.smartroundclinic.payments.domain.model.toRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentRes
import org.slf4j.LoggerFactory

class GetPaymentByIdUseCase(
    private val repository: PaymentRepository,
    private val intaSendRepository: IntaSendRepository,
) {
    private val log = LoggerFactory.getLogger(GetPaymentByIdUseCase::class.java)

    suspend operator fun invoke(id: String): DefaultResponse<PaymentRes?> {
        return when (val result = repository.getById(id)) {
            is Resource.Error -> DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = result.message ?: "Failed to fetch payment",
                data = null,
            )
            is Resource.Success -> {
                val entity = result.data ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Payment not found",
                    data = null,
                )
                DefaultResponse(
                    httpStatusCode = HttpStatusCode.OK.value,
                    status = true,
                    message = "Success",
                    data = reconcileWithInstaSend(entity).toModel().toRes(),
                )
            }
        }
    }

    /**
     * A payment stuck at PENDING/PROCESSING might already be settled at IntaSend if our webhook
     * is late or was dropped — ask IntaSend directly instead of leaving the patient looking at a
     * stale status. Safe to call unconditionally: the write is a no-op once IntaSend still
     * reports a non-terminal state, and can never downgrade an already-COMPLETED payment.
     */
    private suspend fun reconcileWithInstaSend(entity: PaymentEntity): PaymentEntity {
        val invoiceId = entity.invoiceId
        if (invoiceId.isNullOrBlank()) return entity
        if (entity.status != PaymentEntity.PaymentStatus.PENDING &&
            entity.status != PaymentEntity.PaymentStatus.PROCESSING
        ) return entity

        val invoice = when (val result = intaSendRepository.getPaymentStatus(GetPaymentStatusReq(invoiceId))) {
            is Resource.Success -> result.data?.invoice
            is Resource.Error -> {
                log.warn("Live IntaSend status check failed for invoiceId=$invoiceId — ${result.message}")
                null
            }
        } ?: return entity

        val newStatus = when (invoice.state.uppercase()) {
            "COMPLETE" -> PaymentEntity.PaymentStatus.COMPLETED
            "FAILED" -> PaymentEntity.PaymentStatus.FAILED
            else -> return entity
        }

        log.info("Live reconciliation — id=${entity.id} invoiceId=$invoiceId IntaSend state=${invoice.state} -> $newStatus")

        return when (
            val updated = repository.updateFromWebhook(
                id = entity.id,
                status = newStatus.name,
                invoiceId = invoiceId,
                mpesaReference = invoice.mpesaReference,
                charges = invoice.charges.toString(),
                netAmount = invoice.netAmount,
                account = invoice.account,
                paymentMethod = invoice.provider,
            )
        ) {
            is Resource.Success -> updated.data ?: entity
            is Resource.Error -> entity
        }
    }
}
