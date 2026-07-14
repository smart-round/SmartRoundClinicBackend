package ke.co.smartroundclinic.payments.domain.usecase.earnings

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.plugins.BackgroundTask
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory

private const val INTERVAL_SECONDS = 60L
private const val BATCH_SIZE = 25

/**
 * Safety net for [CreditDoctorEarningsUseCase]: sweeps completed payments still missing a credit
 * leg (missed/failed immediate attempt from CompleteAppointmentUseCase, or a payment that confirmed
 * after the appointment was already marked complete) and retries them.
 */
class CreditPendingEarningsTask(
    private val paymentRepository: PaymentRepository,
    private val appointmentInfoLookup: AppointmentInfoLookup,
    private val creditDoctorEarnings: CreditDoctorEarningsUseCase,
) : BackgroundTask {

    private val log = LoggerFactory.getLogger(CreditPendingEarningsTask::class.java)

    override val name = "credit-pending-earnings"
    override val intervalMs = INTERVAL_SECONDS * 1000L

    override suspend fun execute() {
        val pending = (paymentRepository.getCompletedUncredited(BATCH_SIZE) as? Resource.Success)?.data ?: return
        if (pending.isEmpty()) return

        val (missingAppointmentId, withAppointmentId) = pending.partition { it.appointmentId == null }
        missingAppointmentId.forEach { payment ->
            // Can never be resolved via this path — stop retrying it every cycle.
            paymentRepository.markCreditIneligible(payment.id)
            log.warn("[CreditPendingEarnings] paymentId=${payment.id} has no appointmentId, marked credit-ineligible")
        }
        if (withAppointmentId.isEmpty()) return

        // One batched status lookup for the whole sweep instead of one query per payment.
        val statuses = appointmentInfoLookup.getStatuses(withAppointmentId.mapNotNull { it.appointmentId })

        val (terminalNonCompleted, rest) = withAppointmentId.partition {
            val status = statuses[it.appointmentId]
            status == "CANCELLED" || status == "NO_SHOW"
        }
        terminalNonCompleted.forEach { payment ->
            paymentRepository.markCreditIneligible(payment.id)
            log.info("[CreditPendingEarnings] paymentId=${payment.id} appointmentId=${payment.appointmentId} status=${statuses[payment.appointmentId]}, marked credit-ineligible")
        }

        // BOOKED/CONFIRMED appointments haven't happened yet — only COMPLETED ones are actionable now.
        val creditable = rest.filter { statuses[it.appointmentId] == "COMPLETED" }
        if (creditable.isEmpty()) return

        log.info("[CreditPendingEarnings] Retrying ${creditable.size} completed-but-uncredited payment(s)")
        creditable.forEach { payment ->
            val appointmentId = payment.appointmentId!!
            runCatching {
                creditDoctorEarnings.creditEarningsForAppointment(appointmentId, payment.doctorId, statuses[appointmentId])
            }.onFailure { log.error("[CreditPendingEarnings] paymentId=${payment.id} retry failed — ${it.message}", it) }
        }
    }
}
