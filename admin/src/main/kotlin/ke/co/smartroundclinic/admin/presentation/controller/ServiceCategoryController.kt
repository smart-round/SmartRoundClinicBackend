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
import ke.co.smartroundclinic.admin.domain.service.ServiceCategoryService
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateServiceCategoryReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateServiceCategoryReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"
private const val PATIENT = "PATIENT"

fun Route.serviceCategoryController(serviceCategoryService: ServiceCategoryService) {
    authenticate("auth-jwt") {
        route("/admin/service-categories") {
            post {
                call.requireRole(ADMIN) {
                    val body = call.receive<CreateServiceCategoryReq>()
                    val result = serviceCategoryService.create(body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("all") {
                call.requireRole(ADMIN,PATIENT) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = serviceCategoryService.getAll(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get {
                call.requireRole(ADMIN, PATIENT) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val result = serviceCategoryService.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            put {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val body = call.receive<UpdateServiceCategoryReq>()
                    val result = serviceCategoryService.update(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            delete {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val result = serviceCategoryService.delete(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
