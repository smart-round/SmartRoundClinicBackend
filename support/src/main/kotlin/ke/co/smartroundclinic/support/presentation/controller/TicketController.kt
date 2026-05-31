package ke.co.smartroundclinic.support.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.support.domain.service.TicketService
import ke.co.smartroundclinic.support.presentation.dto.request.AssignTicketReq
import ke.co.smartroundclinic.support.presentation.dto.request.CreateTicketReq
import ke.co.smartroundclinic.support.presentation.dto.request.UpdateTicketStatusReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.ticketController(service: TicketService) {
    authenticate("auth-jwt") {
        route("/support/tickets") {

            // POST /support/tickets — create ticket (any authenticated user)
            post {
                call.requireRole(ADMIN, DOCTOR, PATIENT) {
                    val body = call.receive<CreateTicketReq>()
                    val userId = call.getUserId() ?: return@requireRole
                    val result = service.create(body, userId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /support/tickets/mine — my tickets (doctor/patient)
            get("mine") {
                call.requireRole(DOCTOR, PATIENT) {
                    val userId = call.getUserId() ?: return@requireRole
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getMine(userId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /support/tickets/all — all tickets (admin)
            get("all") {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getAll(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /support/tickets?id=xxx — get by ID (admin only)
            get {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("status") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val body = call.receive<UpdateTicketStatusReq>()
                    val result = service.updateStatus(id, body.status)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("assign") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val body = call.receive<AssignTicketReq>()
                    val result = service.assign(id, body.adminUserId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            delete {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val result = service.delete(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
