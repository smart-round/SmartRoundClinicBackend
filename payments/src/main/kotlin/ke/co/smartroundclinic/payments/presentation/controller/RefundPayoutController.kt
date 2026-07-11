package ke.co.smartroundclinic.payments.presentation.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import ke.co.smartroundclinic.payments.data.remote.dto.response.WithdrawalWebhookPayload
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RefundPayoutController")

private val refundGson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .serializeNulls()
    .create()

fun Route.refundPayoutController(intaSendService: IntaSendService, webhookChallenge: String) {
    // Unauthenticated — IntaSend calls this server-to-server after every refund disbursement state change.
    post("/payments/instasend/refund/callback") {
        val raw = call.receiveText()

        val payload = runCatching {
            refundGson.fromJson(raw, WithdrawalWebhookPayload::class.java)
        }.getOrNull()

        if (payload == null) {
            log.warn("Refund webhook rejected — could not parse body")
            call.respond(HttpStatusCode.OK)
            return@post
        }

        if (payload.challenge != webhookChallenge) {
            log.warn("Refund webhook rejected — challenge mismatch (received=${payload.challenge}, expected=***)")
            call.respond(HttpStatusCode.OK)
            return@post
        }

        log.info(
            "Refund webhook authenticated — trackingId=${payload.trackingId} " +
            "status=${payload.status} statusCode=${payload.statusCode} " +
            "totalAmount=${payload.totalAmount} topic=${payload.topic}"
        )

        runCatching { intaSendService.handleRefundWebhook(payload) }
            .onFailure { log.error("Refund webhook handler failed — ${it.message}", it) }

        call.respond(HttpStatusCode.OK)
    }
}
