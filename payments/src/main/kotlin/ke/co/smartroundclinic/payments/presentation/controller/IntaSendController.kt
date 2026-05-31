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
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import ke.co.smartroundclinic.payments.presentation.dto.request.CreateAppointmentPaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.CreatePaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.CreatePreBookingPaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentLinkBody
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("IntaSendController")

// ── HTML page builders ────────────────────────────────────────────────────────

private fun paymentSuccessHtml(invoiceId: String?, amount: String?, currency: String) = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Payment Successful — Smart Round Clinic</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
           background: #f0fdf4; display: flex; align-items: center;
           justify-content: center; min-height: 100vh; padding: 24px; }
    .card { background: #fff; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,.08);
            max-width: 440px; width: 100%; padding: 48px 40px; text-align: center; }
    .icon { font-size: 56px; margin-bottom: 20px; }
    h1 { color: #166534; font-size: 24px; font-weight: 700; margin-bottom: 12px; }
    p  { color: #4b5563; font-size: 15px; line-height: 1.6; }
    .amount { font-size: 32px; font-weight: 800; color: #15803d; margin: 20px 0; }
    .ref { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px;
           padding: 10px 16px; font-size: 13px; color: #166534; margin: 16px 0; word-break: break-all; }
    .footer { margin-top: 32px; font-size: 12px; color: #9ca3af; }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">✅</div>
    <h1>Payment Successful</h1>
    <p>Your payment has been received and confirmed.</p>
    ${if (amount != null) """<div class="amount">$currency $amount</div>""" else ""}
    ${if (invoiceId != null) """<div class="ref">Invoice: $invoiceId</div>""" else ""}
    <p>You can now book your appointment using the Smart Round Clinic app.</p>
    <div class="footer">Smart Round Clinic &bull; Secure Payments</div>
  </div>
</body>
</html>
""".trimIndent()

private fun paymentFailedHtml(reason: String) = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Payment Failed — Smart Round Clinic</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
           background: #fff1f2; display: flex; align-items: center;
           justify-content: center; min-height: 100vh; padding: 24px; }
    .card { background: #fff; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,.08);
            max-width: 440px; width: 100%; padding: 48px 40px; text-align: center; }
    .icon { font-size: 56px; margin-bottom: 20px; }
    h1 { color: #9f1239; font-size: 24px; font-weight: 700; margin-bottom: 12px; }
    p  { color: #4b5563; font-size: 15px; line-height: 1.6; }
    .reason { background: #fff1f2; border: 1px solid #fecdd3; border-radius: 8px;
              padding: 10px 16px; font-size: 13px; color: #9f1239; margin: 16px 0; }
    .footer { margin-top: 32px; font-size: 12px; color: #9ca3af; }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">❌</div>
    <h1>Payment Failed</h1>
    <div class="reason">$reason</div>
    <p>Please go back to the app and try again. If the issue persists, contact support.</p>
    <div class="footer">Smart Round Clinic &bull; Secure Payments</div>
  </div>
</body>
</html>
""".trimIndent()

private fun paymentPendingHtml(invoiceId: String?) = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Payment Pending — Smart Round Clinic</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
           background: #fffbeb; display: flex; align-items: center;
           justify-content: center; min-height: 100vh; padding: 24px; }
    .card { background: #fff; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,.08);
            max-width: 440px; width: 100%; padding: 48px 40px; text-align: center; }
    .icon { font-size: 56px; margin-bottom: 20px; }
    h1 { color: #92400e; font-size: 24px; font-weight: 700; margin-bottom: 12px; }
    p  { color: #4b5563; font-size: 15px; line-height: 1.6; }
    .ref { background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px;
           padding: 10px 16px; font-size: 13px; color: #92400e; margin: 16px 0; }
    .footer { margin-top: 32px; font-size: 12px; color: #9ca3af; }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">⏳</div>
    <h1>Payment Pending</h1>
    <p>Your payment is being processed. This may take a moment.</p>
    ${if (invoiceId != null) """<div class="ref">Invoice: $invoiceId</div>""" else ""}
    <p>Once confirmed, you will receive a notification and can proceed to book your appointment.</p>
    <div class="footer">Smart Round Clinic &bull; Secure Payments</div>
  </div>
</body>
</html>
""".trimIndent()

private fun webhookSuccessHtml(state: String?, invoiceId: String?, value: String?, currency: String?) = """
<!DOCTYPE html><html><head><title>Webhook OK</title></head>
<body style="font-family:monospace;padding:24px">
  <h2 style="color:#166534">&#10003; Webhook processed</h2>
  <p>state=${state ?: "—"} &nbsp; invoiceId=${invoiceId ?: "—"} &nbsp; value=${value ?: "—"} ${currency ?: ""}</p>
</body></html>
""".trimIndent()

private fun webhookErrorHtml(reason: String) = """
<!DOCTYPE html><html><head><title>Webhook Error</title></head>
<body style="font-family:monospace;padding:24px">
  <h2 style="color:#9f1239">&#10007; Webhook error</h2>
  <p>$reason</p>
</body></html>
""".trimIndent()

// Gson with snake_case → camelCase mapping to deserialize IntaSend's webhook body
private val webhookGson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .serializeNulls()
    .create()

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.intaSendController(service: IntaSendService, webhookChallenge: String) {

    // ── Unauthenticated callback endpoints ────────────────────────────────────

    route("/payments/intasend/callback") {

        // POST — IntaSend webhook: server-to-server notification after payment.
        // Read body ONCE as raw text, then parse with Gson (server uses Gson, not kotlinx).
        // Always responds 200 so IntaSend does not retry; body is HTML for observability.
        post {
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

            val handlerError = runCatching { service.handleWebhook(payload) }
                .exceptionOrNull()
                .also { if (it != null) log.error("Webhook handler failed — ${it.message}", it) }

            val html = if (handlerError == null)
                webhookSuccessHtml(payload.state, payload.invoiceId, payload.value, payload.currency)
            else
                webhookErrorHtml(handlerError.message ?: "Internal error while processing webhook")

            call.respondText(html, ContentType.Text.Html, HttpStatusCode.OK)
        }

        // GET — browser redirect after payment; IntaSend appends state and invoice params.
        get {
            val params = call.request.queryParameters
            val state     = params["state"]?.uppercase()
            val invoiceId = params["invoice_id"] ?: params["invoiceId"]
            val value     = params["value"]
            val currency  = params["currency"] ?: "KES"
            val failed    = params["failed_reason"] ?: params["failedReason"]

            log.info("IntaSend redirect callback — state=$state invoiceId=$invoiceId value=$value failed=$failed")

            val html = when (state) {
                "COMPLETE" -> paymentSuccessHtml(invoiceId, value, currency)
                "FAILED"   -> paymentFailedHtml(failed ?: "Your payment could not be processed. Please try again.")
                else       -> paymentPendingHtml(invoiceId)
            }
            call.respondText(html, ContentType.Text.Html, HttpStatusCode.OK)
        }
    }



    // ── Authenticated payment-link management ─────────────────────────────────

    authenticate("auth-jwt") {

        // POST /payments/intasend/appointments
        // Patient creates a payment link for their appointment.
        // Title, ID, amount, and currency are all auto-filled.
        // Response includes data.url — the IntaSend payment page the patient uses.
        post("/payments/intasend/appointments") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val body = call.receive<CreateAppointmentPaymentLinkBody>()
                val result = service.createForAppointment(body, patientId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // POST /payments/intasend/pre-booking?rebooking=true|false
        // Patient creates a payment link BEFORE booking an appointment.
        // Amount is resolved from the doctor's service tier.
        // For rebooking (?rebooking=true), also supply previousAppointmentId in the body to
        // load the follow-up fee; the previous appointment must be COMPLETED and in the same month.
        // The returned transactionRef must be supplied when calling POST /scheduling/appointments.
        post("/payments/intasend/pre-booking") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val isRebooking = call.request.queryParameters["rebooking"]?.lowercase() == "true"
                val body = call.receive<CreatePreBookingPaymentLinkBody>()
                val result = service.createPreBookingLink(
                    doctorId = body.doctorId,
                    patientId = patientId,
                    isRebooking = isRebooking,
                    previousAppointmentId = body.previousAppointmentId,
                )
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        route("/payments/intasend/links") {

            // POST /payments/intasend/links — admin creates a generic payment link
            post {
                call.requireRole(ADMIN) {
                    val body = call.receive<CreatePaymentLinkBody>()
                    val result = service.create(body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/intasend/links?page=1
            get {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val result = service.list(page)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/intasend/links/{id}
            get("{id}") {
                call.requireRole(ADMIN, DOCTOR) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = service.get(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /payments/intasend/links/{id}
            put("{id}") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val body = call.receive<UpdatePaymentLinkBody>()
                    val result = service.update(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
