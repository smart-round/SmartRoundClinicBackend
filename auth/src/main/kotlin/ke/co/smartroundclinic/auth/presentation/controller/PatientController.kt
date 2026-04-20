package ke.co.smartroundclinic.auth.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.service.UserService
import ke.co.smartroundclinic.auth.presentation.dto.request.AdminUpdateUserReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"

fun Route.patientController(userService: UserService) {
    authenticate("auth-jwt") {
        route("/auth/patients") {

            get {
                call.requireRole(ADMIN) {
                    val page = call.parameters["page"]?.toIntOrNull() ?: 1
                    val size = call.parameters["size"]?.toIntOrNull() ?: 20
                    val search = call.parameters["search"]?.takeIf { it.isNotBlank() }
                    val result = userService.getUsersByRole(UserEntity.Role.PATIENT, page, size, search)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("filter") {
                call.requireRole(ADMIN) {
                    val page = call.parameters["page"]?.toIntOrNull() ?: 1
                    val size = call.parameters["size"]?.toIntOrNull() ?: 20
                    val accountStatus = call.parameters["accountStatus"]
                        ?.takeIf { it.isNotBlank() }
                        ?.let { runCatching { UserEntity.AccountStatus.valueOf(it) }.getOrNull() }
                    val createdFrom = call.parameters["createdFrom"]?.takeIf { it.isNotBlank() }
                    val createdTo = call.parameters["createdFrom"]?.takeIf { it.isNotBlank() }
                    val result = userService.filterUsers(
                        role = UserEntity.Role.PATIENT,
                        page = page,
                        size = size,
                        accountStatus = accountStatus,
                        createdFrom = createdFrom,
                        createdTo = createdTo,
                    )
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val body = call.receive<AdminUpdateUserReq>()
                    val result = userService.adminUpdateUser(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
