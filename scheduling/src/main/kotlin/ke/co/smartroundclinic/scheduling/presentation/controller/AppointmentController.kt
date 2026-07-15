package ke.co.smartroundclinic.scheduling.presentation.controller

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
import ke.co.smartroundclinic.infra.plugins.getRole
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.scheduling.domain.service.AppointmentService
import ke.co.smartroundclinic.scheduling.presentation.dto.request.BookAppointmentReq
import ke.co.smartroundclinic.scheduling.presentation.dto.request.CancelAppointmentReq

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.appointmentController(appointmentService: AppointmentService) {
    authenticate("auth-jwt") {
        route("/scheduling/appointments") {

            post {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val body = call.receive<BookAppointmentReq>()
                    val result = appointmentService.book(body, patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/all") {
                call.requireRole(ADMIN) {
                    val result = appointmentService.getAll()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/patient") {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val result = appointmentService.getByPatient(patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
            get("/admin") {
                call.requireRole(ADMIN) {
                    val patientId = call.parameters["patientId"] ?: throw MissingParametersException("patientId is required")
                    val result = appointmentService.getByPatientForAdmin(patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/doctor/all") {
                call.requireRole(DOCTOR) {
                    val id = call.getUserId() ?: return@requireRole
                    val filter = call.request.queryParameters["filter"]
                    val result = appointmentService.getDoctorAppointmentDetails(id, filter)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
            get("/doctor/admin/all") {
                call.requireRole(ADMIN) {
                    val doctorId = call.parameters["doctorId"] ?: throw MissingParametersException("doctorId is required")
                    val filter = call.request.queryParameters["filter"]
                    val result = appointmentService.getDoctorAppointmentDetails(doctorId, filter)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }


            get("/doctor") {
                call.requireRole(DOCTOR, PATIENT) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val date = call.request.queryParameters["date"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "date query parameter is required")
                        return@requireRole
                    }
                    val result = appointmentService.getByDoctor(doctorId, date)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /scheduling/appointments/next?otherUserId=<the doctor/patient on the other side of a chat thread>
            // Chat threads are permanent per doctor-patient pair and can span many appointments over
            // time, so this is the single source of truth clients use to decide whether/when to show
            // the video-call option for a thread — the soonest CONFIRMED appointment that hasn't passed.
            get("/next") {
                call.requireRole(DOCTOR, PATIENT) {
                    val userId = call.getUserId() ?: return@requireRole
                    val role = call.getRole() ?: return@requireRole
                    val otherUserId = call.request.queryParameters["otherUserId"]
                        ?: throw MissingParametersException("otherUserId query parameter is required")
                    val (doctorId, patientId) = if (role.equals(DOCTOR, ignoreCase = true)) userId to otherUserId else otherUserId to userId
                    val result = appointmentService.getNext(doctorId, patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get {
                call.requireRole(DOCTOR, PATIENT) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = appointmentService.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("confirm") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = appointmentService.confirm(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("cancel") {
                call.requireRole(DOCTOR, PATIENT) {
                    val userId = call.getUserId() ?: return@requireRole
                    val role = call.getRole() ?: return@requireRole
                    val id = call.parameters["id"] ?: throw  MissingParametersException("id is required")
                    val body = call.receive<CancelAppointmentReq>()
                    val result = appointmentService.cancel(id, userId, role, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("complete") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = appointmentService.complete(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
