package ke.co.smartroundclinic.scheduling.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.scheduling.domain.service.CalendarService
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.calendarController(calendarService: CalendarService) {

    authenticate("auth-jwt") {
        route("/scheduling/calendar") {

            // GET /scheduling/calendar?doctorId=xxx&view=day|week|month&date=2026-04-21
            // view=month uses date as any day in that month; view=week uses Monday of that week
            get {
                call.requireRole(DOCTOR, PATIENT) {
                    val doctorId = call.request.queryParameters["doctorId"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "doctorId is required")
                        return@requireRole
                    }
                    val dateStr = call.request.queryParameters["date"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "date is required (YYYY-MM-DD)")
                        return@requireRole
                    }
                    val view = call.request.queryParameters["view"] ?: "day"
                    val forDoctor = call.request.queryParameters["forDoctor"]?.toBoolean() ?: false

                    val anchor = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: run {
                        call.respond(HttpStatusCode.BadRequest, "date must be in YYYY-MM-DD format")
                        return@requireRole
                    }

                    val (from, to) = when (view) {
                        "week" -> {
                            val monday = anchor.minus(anchor.dayOfWeek.ordinal)
                            monday to monday.plus(DatePeriod(days = 6))
                        }
                        "month" -> {
                            val first = LocalDate(anchor.year, anchor.month, 1)
                            val last = LocalDate(anchor.year, anchor.month, anchor.month.length(isLeapYear(anchor.year)))
                            first to last
                        }
                        else -> anchor to anchor  // day
                    }

                    val result = calendarService.getCalendar(doctorId, from, to, forDoctor)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }

    // WS /scheduling/calendar/live?doctorId=xxx
    // Pushes a ping frame on each appointment change; client re-fetches the REST endpoint
    webSocket("/scheduling/calendar/live") {
        val doctorId = call.request.queryParameters["doctorId"] ?: run {
            close(io.ktor.websocket.CloseReason(io.ktor.websocket.CloseReason.Codes.VIOLATED_POLICY, "doctorId required"))
            return@webSocket
        }

        calendarService.watchAppointments(doctorId)
            .catch { close(io.ktor.websocket.CloseReason(io.ktor.websocket.CloseReason.Codes.INTERNAL_ERROR, "Stream error")) }
            .collectLatest { appointment ->
                send(Frame.Text(Json.encodeToString(mapOf("event" to "APPOINTMENT_CHANGED", "doctorId" to doctorId, "appointmentId" to appointment.id))))
            }
    }
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun LocalDate.minus(days: Int): LocalDate =
    if (days == 0) this else this.plus(DatePeriod(days = -days))

private fun Month.length(leapYear: Boolean): Int = when (this.number) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (leapYear) 29 else 28
    else -> 30
}
