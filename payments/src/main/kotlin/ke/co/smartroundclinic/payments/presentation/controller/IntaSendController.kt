package ke.co.smartroundclinic.payments.presentation.controller

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
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import ke.co.smartroundclinic.payments.presentation.dto.request.CreatePaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentLinkBody
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("IntaSendController")

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"

fun Route.intaSendController(service: IntaSendService) {

    // ── Unauthenticated callback endpoints ────────────────────────────────────

    route("/payments/intasend/callback") {

        // POST — IntaSend webhook: server-to-server notification after payment
        post {
            val raw = call.receiveText()
            log.info("IntaSend webhook received: $raw")
            runCatching {
                call.receive<IntaSendCallbackPayload>().also { payload ->
                    log.info(
                        "IntaSend callback parsed — invoiceId=${payload.invoiceId} " +
                        "state=${payload.state} value=${payload.value} " +
                        "currency=${payload.currency} account=${payload.account} " +
                        "mpesaRef=${payload.mpesaReference} apiRef=${payload.apiRef}"
                    )
                }
            }.onFailure {
                log.warn("IntaSend callback body could not be parsed as known payload — raw=$raw")
            }
            call.respond(HttpStatusCode.OK)
        }

        // GET — browser redirect after payment; IntaSend appends status query params
        get {
            val params = call.request.queryParameters.entries()
                .joinToString(", ") { (k, v) -> "$k=${v.joinToString(",")}" }
            log.info("IntaSend redirect callback received — params: $params")
            call.respond(HttpStatusCode.OK)
        }
    }

    // ── Authenticated payment-link management ─────────────────────────────────

    authenticate("auth-jwt") {
        route("/payments/intasend/links") {

            // POST /payments/intasend/links — create a new payment link (ADMIN)
            post {
                call.requireRole(ADMIN) {
                    val body = call.receive<CreatePaymentLinkBody>()
                    val result = service.create(body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/intasend/links?page=1 — list all payment links (ADMIN)
            get {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val result = service.list(page)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/intasend/links/{id} — retrieve a single payment link
            get("{id}") {
                call.requireRole(ADMIN, DOCTOR) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = service.get(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /payments/intasend/links/{id} — update a payment link (ADMIN)
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
