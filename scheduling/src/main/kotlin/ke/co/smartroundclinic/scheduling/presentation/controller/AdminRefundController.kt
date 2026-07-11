package ke.co.smartroundclinic.scheduling.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.scheduling.domain.service.AdminRefundService

private const val ADMIN = "ADMIN"

fun Route.adminRefundController(service: AdminRefundService) {
    authenticate("auth-jwt") {
        route("/scheduling/admin/refunds") {

            // GET /scheduling/admin/refunds?page=1&size=20&status=PENDING|COMPLETED|FAILED
            // Admin queue — refund records created when a paid appointment is cancelled by either party.
            get {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val status = call.request.queryParameters["status"]
                    val result = service.getAll(status, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /scheduling/admin/refunds/detail?id=X
            get("detail") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id query parameter is required")
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
