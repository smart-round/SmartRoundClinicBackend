package ke.co.smartroundclinic.admin.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
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
import ke.co.smartroundclinic.infra.plugins.requireImageContentType
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
                        println(body)
                        val result = specialityService.updateSpeciality(id, body)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["id"]
                            ?: throw MissingParametersException("id path parameter is missing")
                        val result = specialityService.deleteSpeciality(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                post("icon") {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["id"]
                            ?: throw MissingParametersException("id path parameter is missing")
                        var imageBytes: ByteArray? = null
                        var contentType = ""
                        call.receiveMultipart().forEachPart { part ->
                            if (part is PartData.FileItem) {
                                imageBytes = part.streamProvider().readBytes()
                                contentType = part.contentType?.toString() ?: ""
                            }
                            part.dispose()
                        }
                        val bytes = imageBytes
                            ?: return@requireRole call.respond(HttpStatusCode.BadRequest, mapOf("message" to "No image file provided"))
                        requireImageContentType(contentType)
                        val result = specialityService.uploadSpecialityIcon(id, bytes, contentType)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete("icon") {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["id"]
                            ?: throw MissingParametersException("id path parameter is missing")
                        val result = specialityService.removeSpecialityIcon(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }
            route("/sub-specialities") {
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
                        val specialityId = call.request.queryParameters["id"]
                            ?: throw MissingParametersException("SpecialityId query parameter is missing")
                        val result = specialityService.getSubSpecialities(specialityId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }
            route("/sub-specialities/{subId}") {
                put {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["subId"]
                            ?: throw MissingParametersException("subId path parameter is missing")
                        val body = call.receive<UpdateSubSpecialityReq>()
                        val result = specialityService.updateSubSpeciality(id, body)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["subId"]
                            ?: throw MissingParametersException("subId path parameter is missing")
                        val result = specialityService.deleteSubSpeciality(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                post("icon") {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["subId"]
                            ?: throw MissingParametersException("subId path parameter is missing")
                        var imageBytes: ByteArray? = null
                        var contentType = ""
                        call.receiveMultipart().forEachPart { part ->
                            if (part is PartData.FileItem) {
                                imageBytes = part.streamProvider().readBytes()
                                contentType = part.contentType?.toString() ?: ""
                            }
                            part.dispose()
                        }
                        val bytes = imageBytes
                            ?: return@requireRole call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("message" to "No image file provided")
                            )
                        requireImageContentType(contentType)
                        val result = specialityService.uploadSubSpecialityIcon(id, bytes, contentType)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete("icon") {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["subId"]
                            ?: throw MissingParametersException("subId path parameter is missing")
                        val result = specialityService.removeSubSpecialityIcon(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }
        }
    }
}
