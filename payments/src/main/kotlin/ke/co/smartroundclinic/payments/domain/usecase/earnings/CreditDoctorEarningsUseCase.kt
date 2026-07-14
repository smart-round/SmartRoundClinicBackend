package ke.co.smartroundclinic.payments.domain.usecase.earnings

import ke.co.smartroundclinic.common.AppointmentEarningsCreditor
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.remote.instasend.request.IntraTransferReq
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.domain.usecase.retryResource
import org.slf4j.LoggerFactory

/** IntaSend rejects internal transfers below this amount ("amount: Ensure this value is greater than or equal to 0.01"). */
private const val MIN_TRANSFERABLE_AMOUNT = 0.01

/**
 * Credits a doctor's IntaSend wallet with their net share of a completed appointment payment,
 * and the platform's commission wallet with the remainder — only once BOTH the payment and the
 * appointment have reached a terminal "done" state. Triggered exactly once, immediately, from
 * CompleteAppointmentUseCase when an appointment is marked COMPLETED — there is no background
 * retry sweep, so each transfer leg gets a short inline retry (see [retryResource]) for transient
 * failures, then gives up and leaves the claim released for a future manual retry if needed.
 */
class CreditDoctorEarningsUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val paymentRepository: PaymentRepository,
    private val walletResolver: DoctorWalletResolver,
    private val appointmentInfoLookup: AppointmentInfoLookup,
    private val config: IntaSendConfig,
) : AppointmentEarningsCreditor {

    private val log = LoggerFactory.getLogger(CreditDoctorEarningsUseCase::class.java)

    override suspend fun creditEarningsForAppointment(appointmentId: String, doctorId: String) {
        val payment = (paymentRepository.getByAppointmentId(appointmentId) as? Resource.Success)?.data
        if (payment == null || payment.status != PaymentEntity.PaymentStatus.COMPLETED) {
            log.info("creditEarningsForAppointment appointmentId=$appointmentId — no completed payment yet, skipping")
            return
        }

        val appointmentStatus = appointmentInfoLookup.getStatus(appointmentId)
        if (appointmentStatus != "COMPLETED") {
            log.info("creditEarningsForAppointment appointmentId=$appointmentId — appointment status=$appointmentStatus, skipping")
            return
        }

        val walletId = walletResolver.resolve(doctorId)
        if (walletId == null) {
            log.warn("creditEarningsForAppointment appointmentId=$appointmentId doctorId=$doctorId — no wallet available, skipping")
            return
        }

        val netAmount = payment.amount * (1.0 - payment.commissionRate / 100.0)
        val commissionAmount = payment.amount - netAmount
        val shortRef = appointmentId.takeLast(6)

        // Names make each wallet's transaction feed self-describing — especially the shared
        // commission wallet, which aggregates cuts from every doctor's appointments in one feed.
        val participants = appointmentInfoLookup.getParticipants(appointmentId)
        val patientLabel = participants?.patientName?.truncateForNarrative() ?: "Patient"
        val doctorLabel = participants?.doctorName?.truncateForNarrative() ?: "Doctor"

        if (payment.doctorCreditedAt == null) {
            creditLeg(
                paymentId = payment.id,
                claim = { paymentRepository.claimDoctorCredit(payment.id) },
                release = { paymentRepository.releaseDoctorCredit(payment.id) },
                destinationWalletId = walletId,
                amount = netAmount,
                narrative = "Consult fee - $patientLabel (APT-$shortRef)",
                legName = "doctor",
            )
        }

        if (payment.commissionCreditedAt == null) {
            creditLeg(
                paymentId = payment.id,
                claim = { paymentRepository.claimCommissionCredit(payment.id) },
                release = { paymentRepository.releaseCommissionCredit(payment.id) },
                destinationWalletId = config.commissionWalletId,
                amount = commissionAmount,
                narrative = "Commission - Dr $doctorLabel (APT-$shortRef)",
                legName = "commission",
            )
        }
    }

    private fun String.truncateForNarrative(maxLength: Int = 40): String =
        if (length <= maxLength) this else take(maxLength - 1) + "…"

    private suspend fun creditLeg(
        paymentId: String,
        claim: suspend () -> Resource<PaymentEntity?>,
        release: suspend () -> Unit,
        destinationWalletId: String,
        amount: Double,
        narrative: String,
        legName: String,
    ) {
        val claimed = (claim() as? Resource.Success)?.data
        if (claimed == null) {
            log.info("creditLeg paymentId=$paymentId leg=$legName — already claimed or payment missing, skipping")
            return
        }

        if (amount < MIN_TRANSFERABLE_AMOUNT) {
            // IntaSend rejects any transfer below its minimum unit — there's genuinely nothing to
            // send (e.g. a 0% commission rate on this payment), so leave the claim in place as done
            // rather than releasing it for a retry that would just fail the same way forever.
            log.info("creditLeg paymentId=$paymentId leg=$legName — amount=$amount below minimum transferable unit, nothing to send")
            return
        }

        val transferResult = retryResource {
            intaSendRepository.internalTransfer(
                originWalletId = config.collectionsWalletId,
                body = IntraTransferReq(
                    destinationWalletId = destinationWalletId,
                    amount = "%.2f".format(amount),
                    narrative = narrative,
                ),
            )
        }

        if (transferResult !is Resource.Success) {
            log.error("creditLeg paymentId=$paymentId leg=$legName — transfer failed after retries: ${(transferResult as? Resource.Error)?.message}, releasing claim for retry")
            release()
            return
        }

        log.info("creditLeg paymentId=$paymentId leg=$legName — credited $amount to wallet=$destinationWalletId")
    }
}
