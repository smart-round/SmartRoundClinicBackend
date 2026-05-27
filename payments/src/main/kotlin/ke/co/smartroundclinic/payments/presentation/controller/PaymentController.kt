package ke.co.smartroundclinic.payments.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.presentation.dto.request.InitiatePaymentReq
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentStatusReq

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.paymentController(service: PaymentService) {
    authenticate("auth-jwt") {
        route("/payments") {

            // POST /payments — Patient initiates a payment for a booking
            post {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val body = call.receive<InitiatePaymentReq>()
                    val result = service.initiate(body, patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments?page=1&size=20&status=PENDING — Admin lists all payments
            get {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val status = call.request.queryParameters["status"]
                    val result = service.getAll(page, size, status)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/{id} — Get payment by id
            get("{id}") {
                call.requireRole(PATIENT, DOCTOR, ADMIN) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/appointment/{appointmentId} — Get payment for a specific appointment
            get("appointment/{appointmentId}") {
                call.requireRole(PATIENT, DOCTOR, ADMIN) {
                    val appointmentId = call.parameters["appointmentId"]
                        ?: throw MissingParametersException("appointmentId is required")
                    val result = service.getByAppointment(appointmentId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/patient?page=1&size=20 — Patient views own payment history
            get("patient") {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getByPatient(patientId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/patient/{patientId}?page=1&size=20 — Admin views a patient's payments
            get("patient/{patientId}") {
                call.requireRole(ADMIN) {
                    val patientId = call.parameters["patientId"]
                        ?: throw MissingParametersException("patientId is required")
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getByPatient(patientId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/doctor?page=1&size=20 — Doctor views own payment history
            get("doctor") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getByDoctor(doctorId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PATCH /payments/{id}/status — Admin updates payment status
            patch("{id}/status") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val body = call.receive<UpdatePaymentStatusReq>()
                    val result = service.updateStatus(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
