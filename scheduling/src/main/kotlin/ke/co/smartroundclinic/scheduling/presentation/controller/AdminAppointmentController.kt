package ke.co.smartroundclinic.scheduling.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.scheduling.domain.service.AdminAppointmentService

private const val ADMIN = "ADMIN"

fun Route.adminAppointmentController(service: AdminAppointmentService) {
    authenticate("auth-jwt") {
        route("/scheduling/admin") {

            // GET /scheduling/admin/stats
            // Platform-wide appointment counts by status, today's bookings, and revenue summary.
            get("stats") {
                call.requireRole(ADMIN) {
                    val result = service.getStats()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /scheduling/admin/appointments?page=1&size=20&status=BOOKED|CONFIRMED|COMPLETED|CANCELLED|NO_SHOW
            // All appointments enriched with doctor name, patient name, and linked payment info.
            // Status filter is case-insensitive. Sorted newest first.
            get("appointments") {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val status = call.request.queryParameters["status"]
                    val result = service.getAll(status, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
