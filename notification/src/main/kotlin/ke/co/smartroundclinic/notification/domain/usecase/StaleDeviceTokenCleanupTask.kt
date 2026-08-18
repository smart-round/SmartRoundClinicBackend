package ke.co.smartroundclinic.notification.domain.usecase

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.plugins.BackgroundTask
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import org.slf4j.LoggerFactory

private const val STALE_AFTER_DAYS = 60
private const val MAX_STANDARD_TOKENS_PER_USER = 5
private const val INTERVAL_HOURS = 24L

/**
 * A device's FCM token rotates over the life of an install, and each reinstall mints a fresh one —
 * registration only ever upserts by exact token value, so a rotated/reinstalled token is a new
 * document, never a replacement of the old one. [UserDeviceTokenRepository.register] caps this going
 * forward on every new registration, but does nothing for accounts that already piled up tokens
 * before that cap existed, or for tokens that just went quiet (app uninstalled, token expired) without
 * ever being re-registered. This sweep catches both.
 */
class StaleDeviceTokenCleanupTask(
    private val tokens: UserDeviceTokenRepository,
) : BackgroundTask {

    private val log = LoggerFactory.getLogger(StaleDeviceTokenCleanupTask::class.java)

    override val name = "stale-device-token-cleanup"
    override val intervalMs = INTERVAL_HOURS * 60 * 60 * 1000L

    override suspend fun execute() {
        when (val result = tokens.pruneStaleAndExcessTokens(STALE_AFTER_DAYS, MAX_STANDARD_TOKENS_PER_USER)) {
            is Resource.Success -> {
                val removed = result.data ?: 0
                if (removed > 0) log.info("[StaleDeviceTokenCleanup] Removed $removed stale/excess device token(s)")
            }
            is Resource.Error -> log.warn("[StaleDeviceTokenCleanup] Prune failed — ${result.message}")
        }
    }
}
