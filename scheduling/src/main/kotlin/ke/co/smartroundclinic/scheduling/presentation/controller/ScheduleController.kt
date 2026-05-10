package ke.co.smartroundclinic.scheduling.presentation.controller

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
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getRole
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.scheduling.domain.service.ScheduleService
import ke.co.smartroundclinic.scheduling.presentation.dto.request.UpdateScheduleDayReq
import ke.co.smartroundclinic.scheduling.presentation.dto.request.UpsertScheduleReq

private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"
private const val ADMIN = "ADMIN"

fun Route.scheduleController(scheduleService: ScheduleService) {
    authenticate("auth-jwt") {
        route("/scheduling/availability") {

            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<UpsertScheduleReq>()
                    val result = scheduleService.upsert(body, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("schedule") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = scheduleService.getSchedule(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get {
                call.requireRole(DOCTOR, PATIENT) {
                    val role = call.getRole()
                    val doctorId = if (role == DOCTOR) {
                        call.getUserId() ?: return@requireRole
                    } else {
                        call.parameters["doctorId"] ?: throw MissingParametersException("doctorId is required")
                    }
                    val date = call.parameters["date"] ?: throw MissingParametersException("date is required in YYYY-MM-DD format")
                    val result = scheduleService.getAvailableSlots(doctorId, date)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            put {
                call.requireRole(DOCTOR, ADMIN) {
                    val id = call.getUserId()
                    val doctorId =  (id ?: call.parameters["doctorId"]) ?: run {
                        call.respond(HttpStatusCode.BadRequest, "doctorId is required")
                        return@requireRole
                    }
                    val day = call.request.queryParameters["day"]?.toIntOrNull() ?: run {
                        call.respond(HttpStatusCode.BadRequest, "day query parameter (0-6) is required")
                        return@requireRole
                    }
                    val body = call.receive<UpdateScheduleDayReq>()
                    val result = scheduleService.updateDay(doctorId, day, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            delete {
                call.requireRole(DOCTOR, ADMIN) {
                    val id = call.getUserId()
                    val doctorId =  (id ?: call.parameters["doctorId"]) ?: run {
                        call.respond(HttpStatusCode.BadRequest, "doctorId is required")
                        return@requireRole
                    }
                    val day = call.request.queryParameters["day"]?.toIntOrNull() ?: run {
                        call.respond(HttpStatusCode.BadRequest, "day query parameter (0-6) is required")
                        return@requireRole
                    }
                    val result = scheduleService.deactivateDay(doctorId, day)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
