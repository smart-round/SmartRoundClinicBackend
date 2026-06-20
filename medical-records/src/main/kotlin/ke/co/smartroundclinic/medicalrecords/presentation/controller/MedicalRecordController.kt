package ke.co.smartroundclinic.medicalrecords.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.medicalrecords.domain.service.MedicalRecordService
import ke.co.smartroundclinic.medicalrecords.presentation.dto.request.SaveMedicalRecordReq

private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.medicalRecordController(
    service: MedicalRecordService,
    getSenderName: suspend (String) -> String?,
) {
    authenticate("auth-jwt") {
        route("/medical-records") {
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<SaveMedicalRecordReq>()
                    val name = getSenderName(doctorId) ?: "Doctor"
                    val result = service.save(body, doctorId, name)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/appointment/{appointmentId}") {
                call.requireRole(DOCTOR, PATIENT) {
                    val appointmentId = call.parameters["appointmentId"]
                        ?: throw MissingParametersException("appointmentId is required")
                    val result = service.getByAppointment(appointmentId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/patient") {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val result = service.getPatientHistory(patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/patient/{patientId}") {
                call.requireRole(DOCTOR) {
                    val patientId = call.parameters["patientId"]
                        ?: throw MissingParametersException("patientId is required")
                    val result = service.getPatientHistory(patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
