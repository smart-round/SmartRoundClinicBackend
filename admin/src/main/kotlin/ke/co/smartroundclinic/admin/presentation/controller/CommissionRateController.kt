package ke.co.smartroundclinic.admin.presentation.controller

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
import ke.co.smartroundclinic.admin.domain.service.CommissionRateService
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateCommissionRateReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateCommissionRateReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"

fun Route.commissionRateController(service: CommissionRateService) {
    authenticate("auth-jwt") {
        route("/admin/commission-rates") {
            post {
                call.requireRole(ADMIN) {
                    val adminId = call.getUserId()
                        ?: throw MissingParametersException("Could not resolve admin id from token")
                    val body = call.receive<CreateCommissionRateReq>()
                    val result = service.create(body, adminId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get {
                call.requireRole(ADMIN) {
                    val result = service.getById()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            put {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val body = call.receive<UpdateCommissionRateReq>()
                    val result = service.update(id, body)
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
