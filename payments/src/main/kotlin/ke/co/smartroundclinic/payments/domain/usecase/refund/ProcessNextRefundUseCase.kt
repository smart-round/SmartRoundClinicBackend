package ke.co.smartroundclinic.payments.domain.usecase.refund

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateMpesaB2CRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateMpesaB2CTransaction
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.toApproveMpesaB2CRequest
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.RefundRepository
import org.slf4j.LoggerFactory

/**
 * Processes a single pending refund via M-Pesa B2C — the patient's paying phone number is
 * resolved from the original payment (the same `account` field STK push confirmation writes).
 * Invoked one refund at a time by [ProcessRefundsTask].
 */
class ProcessNextRefundUseCase(
    private val refundRepository: RefundRepository,
    private val paymentRepository: PaymentRepository,
    private val intaSendRepository: IntaSendRepository,
    private val appointmentInfoLookup: AppointmentInfoLookup,
    private val config: IntaSendConfig,
) {
    private val log = LoggerFactory.getLogger(ProcessNextRefundUseCase::class.java)

    /** Returns true if a refund was picked up and attempted (regardless of outcome). */
    suspend operator fun invoke(): Boolean {
        val refund = (refundRepository.getNextPending() as? Resource.Success)?.data ?: return false

        val payment = (paymentRepository.getById(refund.paymentId) as? Resource.Success)?.data
        val phoneNumber = payment?.account
        if (phoneNumber.isNullOrBlank()) {
            log.warn("Refund id=${refund.id} — no phone number on file for paymentId=${refund.paymentId}, marking FAILED")
            refundRepository.markFailed(refund.id, "No phone number on file for the original payment")
            return true
        }

        val patientName = appointmentInfoLookup.getParticipants(refund.appointmentId)?.patientName ?: "Refund Recipient"

        val initiateResult = intaSendRepository.createMpesaB2CRequest(
            CreateMpesaB2CRequestReq(
                callbackUrl = config.callBackRefundUrl,
                transactions = listOf(
                    CreateMpesaB2CTransaction(
                        account = phoneNumber,
                        amount = refund.amount.toInt().toString(),
                        name = patientName,
                        narrative = "Refund for cancelled appointment ${refund.appointmentId}",
                    )
                ),
            )
        )

        if (initiateResult !is Resource.Success || initiateResult.data == null) {
            val reason = (initiateResult as? Resource.Error)?.message ?: "Failed to initiate refund with payment provider"
            log.warn("Refund id=${refund.id} — initiate failed: $reason")
            refundRepository.markFailed(refund.id, reason)
            return true
        }

        val initiated = initiateResult.data!!
        val approveResult = intaSendRepository.approveMpesaB2CRequest(initiated.toApproveMpesaB2CRequest())

        if (approveResult !is Resource.Success) {
            val reason = (approveResult as? Resource.Error)?.message ?: "Failed to approve refund with payment provider"
            log.warn("Refund id=${refund.id} — approve failed: $reason")
            refundRepository.markFailed(refund.id, reason)
            return true
        }

        log.info("Refund id=${refund.id} submitted — trackingId=${initiated.trackingId} phoneNumber=$phoneNumber amount=${refund.amount}")
        refundRepository.markSubmitted(refund.id, initiated.trackingId, phoneNumber)
        return true
    }
}
