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
import ke.co.smartroundclinic.notification.presentation.dto.request.RegisterDeviceTokenReq
import ke.co.smartroundclinic.notification.presentation.dto.request.SendPushNotificationReq

private const val ADMIN = "ADMIN"

private fun roleToDestination(role: String?): NotificationDestination = when (role) {
    "ADMIN", "SUPER_ADMIN" -> NotificationDestination.ADMIN
    "PATIENT"              -> NotificationDestination.PATIENT
    else                   -> NotificationDestination.DOCTOR
}

fun Route.notificationController(service: NotificationService) {
    authenticate("auth-jwt") {

        // ── In-app notifications ───────────────────────────────────────────────
        route("/notification") {

            post {
                call.requirePermission(ADMIN) {
                    val req = call.receive<CreateNotificationReq>()
                    val result = service.create(req)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("all") {
                call.requirePermission(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getAll(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            delete {
                call.requirePermission(ADMIN) {
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val result = service.delete(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("my") {
                val userId = call.getUserId() ?: return@get
                val destination = roleToDestination(call.getRole())
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val result = service.getMy(userId, destination, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            patch("read") {
                val userId = call.getUserId() ?: return@patch
                val destination = roleToDestination(call.getRole())
                val id = call.request.queryParameters["id"]
                    ?: throw MissingParametersException("id query parameter is missing")
                val result = service.markAsRead(id, userId, destination)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            get {
                val id = call.request.queryParameters["id"]
                    ?: throw MissingParametersException("id query parameter is missing")
                val result = service.getById(id)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // ── Device token management ────────────────────────────────────────────
        route("/notification/device-token") {

            // Register FCM token for the authenticated user
            post {
                val userId = call.getUserId() ?: return@post
                val role = call.getRole() ?: "DOCTOR"
                val req = call.receive<RegisterDeviceTokenReq>()
                val result = service.registerDeviceToken(req, userId, role)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // Unregister a specific FCM token (owner only)
            delete {
                val userId = call.getUserId() ?: return@delete
                val tokenId = call.request.queryParameters["tokenId"]
                    ?: throw MissingParametersException("tokenId query parameter is missing")
                val result = service.unregisterDeviceToken(tokenId, userId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // ── Push notifications ─────────────────────────────────────────────────
        route("/notification/push") {

            // Send push: recipientId (specific user) or destination (DOCTOR/PATIENT/ALL)
            post {
                call.requirePermission("SUPER_ADMIN",ADMIN) {
                    val req = call.receive<SendPushNotificationReq>()
                    val result = service.sendPush(req)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // Push notification dispatch logs
            get("logs") {
                call.requirePermission(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getPushLogs(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
