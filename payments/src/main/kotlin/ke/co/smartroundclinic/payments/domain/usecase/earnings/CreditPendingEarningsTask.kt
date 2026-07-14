package ke.co.smartroundclinic.payments.domain.usecase.earnings

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.plugins.BackgroundTask
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
    private val creditDoctorEarnings: CreditDoctorEarningsUseCase,
) : BackgroundTask {

    private val log = LoggerFactory.getLogger(CreditPendingEarningsTask::class.java)

    override val name = "credit-pending-earnings"
    override val intervalMs = INTERVAL_SECONDS * 1000L

    override suspend fun execute() {
        val pending = (paymentRepository.getCompletedUncredited(BATCH_SIZE) as? Resource.Success)?.data ?: return
        if (pending.isEmpty()) return

        log.info("[CreditPendingEarnings] Retrying ${pending.size} completed-but-uncredited payment(s)")
        pending.forEach { payment ->
            val appointmentId = payment.appointmentId
            if (appointmentId == null) {
                log.warn("[CreditPendingEarnings] paymentId=${payment.id} has no appointmentId, skipping")
                return@forEach
            }
            runCatching { creditDoctorEarnings.creditEarningsForAppointment(appointmentId, payment.doctorId) }
                .onFailure { log.error("[CreditPendingEarnings] paymentId=${payment.id} retry failed — ${it.message}", it) }
        }
    }
}
