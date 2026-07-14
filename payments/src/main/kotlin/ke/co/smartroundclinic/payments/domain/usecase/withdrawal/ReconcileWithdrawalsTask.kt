package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.plugins.BackgroundTask
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CheckSendMoneyStatusReq
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Instant

private const val INTERVAL_SECONDS = 60L
private const val STALE_AFTER_SECONDS = 120L

/**
 * Safety net for withdrawals whose IntaSend webhook (POST /payments/instasend/withdrawal/callback)
 * never arrived: re-checks stale PENDING withdrawals against IntaSend's status endpoint and
 * persists the result — closes the previous gap where CheckWithdrawalStatusUseCase checked status
 * but never wrote it back.
 */
class ReconcileWithdrawalsTask(
    private val withdrawalRepository: WithdrawalRepository,
    private val intaSendRepository: IntaSendRepository,
) : BackgroundTask {

    private val log = LoggerFactory.getLogger(ReconcileWithdrawalsTask::class.java)

    override val name = "reconcile-withdrawals"
    override val intervalMs = INTERVAL_SECONDS * 1000L

    override suspend fun execute() {
        val pending = (withdrawalRepository.getAllForAdmin(WithdrawalEntity.WithdrawalStatus.PENDING.name) as? Resource.Success)?.data
            ?: return
        val now = Clock.System.now()
        val stale = pending.filter { withdrawal ->
            val createdAt = runCatching { Instant.parse(withdrawal.createdAt) }.getOrNull() ?: return@filter true
            (now - createdAt).inWholeSeconds >= STALE_AFTER_SECONDS
        }
        if (stale.isEmpty()) return

        log.info("[ReconcileWithdrawals] Re-checking ${stale.size} stale pending withdrawal(s)")
        stale.forEach { withdrawal ->
            val statusResult = intaSendRepository.checkSendMoneyStatus(CheckSendMoneyStatusReq(withdrawal.trackingId))
            val status = (statusResult as? Resource.Success)?.data ?: return@forEach
            val newStatus = when (status.statusCode) {
                "BP200" -> WithdrawalEntity.WithdrawalStatus.COMPLETED.name
                "BP400" -> WithdrawalEntity.WithdrawalStatus.FAILED.name
                else -> return@forEach
            }
            withdrawalRepository.updateStatus(withdrawal.trackingId, newStatus)
            log.info("[ReconcileWithdrawals] trackingId=${withdrawal.trackingId} reconciled to $newStatus")
        }
    }
}
