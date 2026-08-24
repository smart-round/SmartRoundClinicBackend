package ke.co.smartroundclinic.infra.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimiting() {
    install(RateLimit) {
        global {
            rateLimiter(limit = 10, refillPeriod = 1.seconds)
            // Bucket per client + endpoint, so one caller hammering one API can't
            // exhaust the quota other callers or other endpoints rely on.
            requestKey { call -> "${call.request.origin.remoteHost}:${call.request.path()}" }
        }
    }
}
