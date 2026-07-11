package ke.co.smartroundclinic.payments.domain.usecase.refund

import ke.co.smartroundclinic.infra.plugins.BackgroundTask
import org.slf4j.LoggerFactory

private const val INTERVAL_SECONDS = 30L

/** Picks up and disburses one PENDING refund per tick — refunds are processed one at a time. */
class ProcessRefundsTask(
    private val processNextRefund: ProcessNextRefundUseCase,
) : BackgroundTask {

    private val log = LoggerFactory.getLogger(ProcessRefundsTask::class.java)

    override val name = "process-refunds"
    override val intervalMs = INTERVAL_SECONDS * 1000L

    override suspend fun execute() {
        val processed = processNextRefund()
        if (processed) log.info("[ProcessRefunds] Picked up and attempted one pending refund")
    }
}
