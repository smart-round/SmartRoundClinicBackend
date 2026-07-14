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
import org.slf4j.LoggerFactory

/**
 * Credits a doctor's IntaSend wallet with their net share of a completed appointment payment,
 * and the platform's commission wallet with the remainder — only once BOTH the payment and the
 * appointment have reached a terminal "done" state. Safe to call repeatedly (from the immediate
 * CompleteAppointmentUseCase hook, or from CreditPendingEarningsTask's sweep) — each of the two
 * transfer legs is claimed atomically on PaymentEntity so a retry never double-credits a leg that
 * already succeeded, and only resumes the leg that didn't.
 */
class CreditDoctorEarningsUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val paymentRepository: PaymentRepository,
    private val walletResolver: DoctorWalletResolver,
    private val appointmentInfoLookup: AppointmentInfoLookup,
    private val config: IntaSendConfig,
) : AppointmentEarningsCreditor {

    private val log = LoggerFactory.getLogger(CreditDoctorEarningsUseCase::class.java)

    override suspend fun creditEarningsForAppointment(appointmentId: String, doctorId: String) =
        creditEarningsForAppointment(appointmentId, doctorId, knownStatus = null)

    /**
     * Same as [creditEarningsForAppointment], but takes an already-fetched appointment status so
     * batch callers (CreditPendingEarningsTask) can look up statuses for many appointments in one
     * query instead of one [AppointmentInfoLookup.getStatus] call per payment.
     */
    suspend fun creditEarningsForAppointment(appointmentId: String, doctorId: String, knownStatus: String?) {
        val payment = (paymentRepository.getByAppointmentId(appointmentId) as? Resource.Success)?.data
        if (payment == null || payment.status != PaymentEntity.PaymentStatus.COMPLETED) {
            log.info("creditEarningsForAppointment appointmentId=$appointmentId — no completed payment yet, skipping")
            return
        }

        val appointmentStatus = knownStatus ?: appointmentInfoLookup.getStatus(appointmentId)
        if (appointmentStatus != "COMPLETED") {
            if (appointmentStatus == "CANCELLED" || appointmentStatus == "NO_SHOW") {
                // Terminal state that will never become COMPLETED — stop retrying this payment.
                paymentRepository.markCreditIneligible(payment.id)
                log.info("creditEarningsForAppointment appointmentId=$appointmentId — appointment status=$appointmentStatus, marked credit-ineligible")
            } else {
                log.debug("creditEarningsForAppointment appointmentId=$appointmentId — appointment status=$appointmentStatus, not yet completed")
            }
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

        val transferResult = intaSendRepository.internalTransfer(
            originWalletId = config.collectionsWalletId,
            body = IntraTransferReq(
                destinationWalletId = destinationWalletId,
                amount = "%.2f".format(amount),
                narrative = narrative,
            ),
        )

        if (transferResult !is Resource.Success) {
            log.error("creditLeg paymentId=$paymentId leg=$legName — transfer failed: ${(transferResult as? Resource.Error)?.message}, releasing claim for retry")
            release()
            return
        }

        log.info("creditLeg paymentId=$paymentId leg=$legName — credited $amount to wallet=$destinationWalletId")
    }
}
