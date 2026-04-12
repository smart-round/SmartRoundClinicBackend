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
import ke.co.smartroundclinic.admin.domain.service.SpecialityService
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateSubSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateSpecialityReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateSubSpecialityReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"

fun Route.specialityController(specialityService: SpecialityService) {
    authenticate("auth-jwt") {
        route("/admin/specialities") {
            post {
                call.requireRole(ADMIN) {
                    val body = call.receive<List<CreateSpecialityReq>>()
                    val result = specialityService.createSpeciality(body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get {
                call.requireRole(ADMIN) {
                    val result = specialityService.getSpecialities()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            route("{id}") {
                get {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["id"]
                            ?: throw MissingParametersException("id path parameter is missing")
                        val result = specialityService.getSpecialityById(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                put {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["id"]
                            ?: throw MissingParametersException("id path parameter is missing")
                        val body = call.receive<UpdateSpecialityReq>()
                        val result = specialityService.updateSpeciality(id, body)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                route("subspecialities") {
                    post {
                        call.requireRole(ADMIN) {
                            val specialityId = call.parameters["id"]
                                ?: throw MissingParametersException("id path parameter is missing")
                            val body = call.receive<CreateSubSpecialityReq>()
                            val result = specialityService.createSubSpeciality(specialityId, body)
                            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                        }
                    }

                    get {
                        call.requireRole(ADMIN) {
                            val specialityId = call.parameters["id"]
                                ?: throw MissingParametersException("id path parameter is missing")
                            val result = specialityService.getSubSpecialities(specialityId)
                            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                        }
                    }
                }
            }
        }

        route("/admin/subspecialities/{id}") {
            put {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val body = call.receive<UpdateSubSpecialityReq>()
                    val result = specialityService.updateSubSpeciality(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            delete {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is missing")
                    val result = specialityService.deleteSubSpeciality(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
