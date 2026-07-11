package ke.co.smartroundclinic.payments.presentation.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import ke.co.smartroundclinic.payments.presentation.dto.request.StkPushAppointmentBody
import ke.co.smartroundclinic.payments.presentation.dto.request.StkPushPreBookingBody
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("IntaSendController")

/** Minimal diagnostic page for malformed/rejected webhook deliveries — not user-facing. */
private fun webhookErrorHtml(reason: String) = """
<!DOCTYPE html><html><head><meta charset="UTF-8"/><title>Webhook Error</title>
<style>body{font-family:monospace;background:#fef2f2;padding:32px;color:#991b1b}
.box{background:#fff;border:1px solid #fca5a5;border-radius:8px;padding:20px;max-width:500px}
h2{margin-bottom:12px}p{color:#374151;font-size:13px}</style></head>
<body><div class="box">
  <h2>&#10007; Webhook error</h2>
  <p>$reason</p>
</div></body></html>
""".trimIndent()

// Gson with snake_case → camelCase mapping to deserialize IntaSend's webhook body
private val webhookGson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .serializeNulls()
    .create()

private const val PATIENT = "PATIENT"

fun Route.intaSendController(service: IntaSendService, webhookChallenge: String) {

    // POST /payments/intasend/callback — IntaSend webhook: server-to-server notification after
    // payment. Read body ONCE as raw text, then parse with Gson (server uses Gson, not kotlinx).
    // Always responds 200 so IntaSend does not retry.
    post("/payments/intasend/callback") {
        val raw = call.receiveText()
        log.info("IntaSend webhook received: $raw")

        val payload = runCatching {
            webhookGson.fromJson(raw, IntaSendCallbackPayload::class.java)
        }.getOrNull()

        if (payload == null) {
            log.warn("IntaSend webhook could not be parsed — raw=$raw")
            call.respondText(webhookErrorHtml("Could not parse webhook payload"), ContentType.Text.Html, HttpStatusCode.OK)
            return@post
        }

        if (payload.challenge != webhookChallenge) {
            log.warn("IntaSend webhook rejected — challenge mismatch (received=${payload.challenge})")
            call.respondText(webhookErrorHtml("Invalid webhook challenge"), ContentType.Text.Html, HttpStatusCode.OK)
            return@post
        }

        runCatching { service.handleWebhook(payload) }
            .onFailure { log.error("Webhook handler failed — ${it.message}", it) }

        call.respond(HttpStatusCode.OK)
    }

    authenticate("auth-jwt") {

        // POST /payments/intasend/appointments/stk-push
        // Patient triggers an M-Pesa STK push for an existing appointment.
        // Returns invoice_id (for polling) and transaction_ref (for booking) in the response.
        post("/payments/intasend/appointments/stk-push") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val body = call.receive<StkPushAppointmentBody>()
                val result = service.stkPushForAppointment(body.appointmentId, body.phoneNumber, patientId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // POST /payments/intasend/pre-booking/stk-push?rebooking=true|false
        // Patient triggers an M-Pesa STK push BEFORE booking an appointment.
        // Returns invoice_id (for polling) and transaction_ref (for booking) in the response.
        post("/payments/intasend/pre-booking/stk-push") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val isRebooking = call.request.queryParameters["rebooking"]?.lowercase() == "true"
                val body = call.receive<StkPushPreBookingBody>()
                val result = service.stkPushPreBooking(
                    doctorId = body.doctorId,
                    patientId = patientId,
                    phoneNumber = body.phoneNumber,
                    isRebooking = isRebooking,
                    previousAppointmentId = body.previousAppointmentId,
                )
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // GET /payments/intasend/stk-push/status?invoiceId=X
        // Poll the status of an STK push payment by invoice ID.
        get("/payments/intasend/stk-push/status") {
            val invoiceId = call.request.queryParameters["invoiceId"]
                ?: throw MissingParametersException("invoiceId is required")
            val result = service.getStkPushStatus(invoiceId)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }
    }
}
