package ke.co.smartroundclinic.payments.presentation.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import ke.co.smartroundclinic.payments.data.remote.dto.response.ChargebackWebhookPayload
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RefundPayoutController")

private val chargebackGson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .serializeNulls()
    .create()

fun Route.refundPayoutController(intaSendService: IntaSendService, webhookChallenge: String) {
    // Unauthenticated — IntaSend calls this server-to-server whenever a chargeback's status
    // changes. Chargebacks are a separate event category from send-money disbursements, so this
    // is a dedicated callback rather than sharing the withdrawal one.
    post("/payments/instasend/refund/callback") {
        val raw = call.receiveText()

        val payload = runCatching {
            chargebackGson.fromJson(raw, ChargebackWebhookPayload::class.java)
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

        log.info("Refund webhook authenticated — chargebackId=${payload.chargebackId} status=${payload.status} amount=${payload.amount}")

        runCatching { intaSendService.handleRefundWebhook(payload) }
            .onFailure { log.error("Refund webhook handler failed — ${it.message}", it) }

        call.respond(HttpStatusCode.OK)
    }
}
