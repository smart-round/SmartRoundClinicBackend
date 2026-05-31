package ke.co.smartroundclinic.payments.presentation.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
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
        post {
            val raw = call.receiveText()
            log.info("IntaSend webhook received: $raw")

            val payload = runCatching {
                webhookGson.fromJson(raw, IntaSendCallbackPayload::class.java)
            }.getOrNull()

            if (payload == null) {
                log.warn("IntaSend webhook could not be parsed — raw=$raw")
                call.respond(HttpStatusCode.OK)
                return@post
            }

            if (payload.challenge != webhookChallenge) {
                log.warn("IntaSend webhook rejected — challenge mismatch (received=${payload.challenge})")
                call.respond(HttpStatusCode.OK)
                return@post
            }

            runCatching { service.handleWebhook(payload) }
                .onFailure { log.error("Webhook handler failed — ${it.message}", it) }

            call.respond(HttpStatusCode.OK)
        }

        // GET — browser redirect after payment; IntaSend appends status query params
        get {
            val params = call.request.queryParameters.entries()
                .joinToString(", ") { (k, v) -> "$k=${v.joinToString(",")}" }
            log.info("IntaSend redirect callback — params: $params")
            call.respond(HttpStatusCode.OK)
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

        // POST /payments/intasend/pre-booking
        // Patient creates a payment link BEFORE booking an appointment.
        // Amount is resolved from the doctor's service tier. The returned transactionRef
        // must be supplied when calling POST /scheduling/appointments.
        post("/payments/intasend/pre-booking") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val body = call.receive<CreatePreBookingPaymentLinkBody>()
                val result = service.createPreBookingLink(body.doctorId, patientId)
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
