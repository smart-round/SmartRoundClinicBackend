package ke.co.smartroundclinic.payments.domain.usecase

import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.delay

/**
 * Retries a Resource-returning IntaSend call a couple of times with a short delay in place, for
 * transient failures (network blip, momentary 5xx) at the moment of an immediate trigger
 * (appointment completed/cancelled, withdrawal requested) — not a background retry loop. Gives up
 * and returns the last failure after [attempts] tries.
 */
internal suspend fun <T> retryResource(
    attempts: Int = 3,
    delayMs: Long = 500,
    block: suspend () -> Resource<T>,
): Resource<T> {
    var last = block()
    repeat(attempts - 1) {
        if (last is Resource.Success) return last
        delay(delayMs)
        last = block()
    }
    return last
}
