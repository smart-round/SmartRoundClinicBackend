package ke.co.smartroundclinic.doctor.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.doctor.domain.service.PaymentDetailsService
import ke.co.smartroundclinic.doctor.presentation.dto.request.AddPaymentDetailsReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdatePaymentDetailsReq

import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"
private const val ADMIN = "ADMIN"

fun Route.paymentDetailsController(service: PaymentDetailsService) {
    authenticate("auth-jwt") {

        // ── Doctor-only: manage own payment details ───────────────────────────
        route("/doctor/payment-details") {

            // POST /doctor/payment-details
            // Add payment details for the authenticated doctor.
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<AddPaymentDetailsReq>()
                    val result = service.add(body.toModel(doctorId).toEntity())
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/payment-details/me
            // Fetch the authenticated doctor's own payment details.
            get("me") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.get(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /doctor/payment-details
            // Update the authenticated doctor's payment details.
            put {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<UpdatePaymentDetailsReq>()
                    val result = service.update(doctorId, body.accountName, body.accountNumber, body.bankCode, body.branchCode, body.bankName, body.branchName)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // DELETE /doctor/payment-details
            // Remove the authenticated doctor's payment details.
            delete {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.delete(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }

        // ── Admin-only: browse all payment details ────────────────────────────
        route("/admin/payment-details") {

            // GET /admin/payment-details?page=1&size=20
            // Paginated list of all practitioners' payment details.
            get {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getAll(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /admin/payment-details/{id}
            // Get a single payment-details record by its id.

            get("{id}") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"] 
                        ?: throw MissingParametersException("id path parameter is required")
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}