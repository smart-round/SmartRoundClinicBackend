package ke.co.smartroundclinic.notification.presentation.controller

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
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.getRole
import ke.co.smartroundclinic.infra.plugins.requirePermission
import ke.co.smartroundclinic.notification.domain.service.NotificationService
import ke.co.smartroundclinic.notification.presentation.dto.request.CreateNotificationReq

private const val ADMIN = "ADMIN"

private fun roleToDestination(role: String?): NotificationDestination = when (role) {
    "ADMIN", "SUPER_ADMIN" -> NotificationDestination.ADMIN
    "PATIENT"              -> NotificationDestination.PATIENT
    else                   -> NotificationDestination.DOCTOR
}

fun Route.notificationController(service: NotificationService) {
    authenticate("auth-jwt") {
        route("/notification") {

            // ── Admin: send a notification (requires MANAGE_NOTIFICATIONS) ────
            post {
                call.requirePermission(ADMIN) {
                    val req = call.receive<CreateNotificationReq>()
                    val result = service.create(req)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // ── Admin: get all notifications (requires MANAGE_NOTIFICATIONS) ──
            get("all") {
                call.requirePermission(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getAll(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // ── Admin: delete a notification (requires MANAGE_NOTIFICATIONS) ──
            delete {
                call.requirePermission(ADMIN) {
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val result = service.delete(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // ── Any JWT: get own notifications (targeted + broadcasts for role) ──
            get("my") {
                val userId = call.getUserId() ?: return@get
                val destination = roleToDestination(call.getRole())
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val result = service.getMy(userId, destination, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // ── Any JWT: mark own notification as read ────────────────────────
            patch("read") {
                val userId = call.getUserId() ?: return@patch
                val destination = roleToDestination(call.getRole())
                val id = call.request.queryParameters["id"]
                    ?: throw MissingParametersException("id query parameter is missing")
                val result = service.markAsRead(id, userId, destination)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // ── Any JWT: get single notification ──────────────────────────────
            get {
                val id = call.request.queryParameters["id"]
                    ?: throw MissingParametersException("id query parameter is missing")
                val result = service.getById(id)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }
    }
}
