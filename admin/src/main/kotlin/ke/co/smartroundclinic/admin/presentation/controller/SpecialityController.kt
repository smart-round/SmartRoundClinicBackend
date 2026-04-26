package ke.co.smartroundclinic.admin.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
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
import ke.co.smartroundclinic.infra.plugins.requireImageContentType
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"

fun Route.specialityController(specialityService: SpecialityService) {
    authenticate("auth-jwt") {
        route("/admin/specialities") {
            post {
                call.requireRole(ADMIN) {
                    val parts = mutableMapOf<String, String>()
                    var imageBytes: ByteArray? = null
                    var contentType = ""
                    call.receiveMultipart().forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> parts[part.name ?: ""] = part.value
                            is PartData.FileItem -> {
                                imageBytes = part.streamProvider().readBytes()
                                contentType = part.contentType?.toString() ?: ""
                            }

                            else -> Unit
                        }
                        part.dispose()
                    }
                    val title = parts["title"]
                        ?: return@requireRole call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("message" to "title is required")
                        )
                    val description = parts["description"]
                        ?: return@requireRole call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("message" to "description is required")
                        )
                    if (imageBytes != null) requireImageContentType(contentType)
                    val req = CreateSpecialityReq(
                        serviceTierId = parts["serviceTierId"],
                        title = title,
                        description = description,
                        color = parts["color"]?.trim() ?: "#FFFFFF",
                    )
                    val result = specialityService.createSpeciality(req, imageBytes, contentType.ifBlank { null })
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("all") {
                call.requireRole(ADMIN) {
                    val result = specialityService.getSpecialities()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }


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
                    val parts = mutableMapOf<String, String>()
                    var imageBytes: ByteArray? = null
                    var contentType = ""
                    call.receiveMultipart().forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> parts[part.name ?: ""] = part.value
                            is PartData.FileItem -> {
                                imageBytes = part.streamProvider().readBytes()
                                contentType = part.contentType?.toString() ?: ""
                            }

                            else -> Unit
                        }
                        part.dispose()
                    }
                    if (imageBytes != null) requireImageContentType(contentType)
                    val req = UpdateSpecialityReq(
                        serviceTierId = parts["serviceTierId"],
                        title = parts["title"],
                        description = parts["description"],
                        color = parts["color"]?.trim(),
                    )
                    val result = specialityService.updateSpeciality(id, req, imageBytes, contentType.ifBlank { null })
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


            route("service-tier") {
                post {
                    call.requireRole(ADMIN) {
                        val specialityId = call.parameters["specialityId"]
                            ?: throw MissingParametersException("specialityId path parameter is missing")
                        val serviceTierId = call.parameters["serviceTierId"]
                            ?: throw MissingParametersException("serviceTierId path parameter is missing")
                        val result = specialityService.assignToServiceTier(specialityId, serviceTierId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete {
                    call.requireRole(ADMIN) {
                        val specialityId = call.parameters["specialityId"]
                            ?: throw MissingParametersException("specialityId path parameter is missing")
                        val result = specialityService.unassignFromServiceTier(specialityId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }

            route("service-category") {
                post {
                    call.requireRole(ADMIN) {
                        val specialityId = call.parameters["specialityId"]
                            ?: throw MissingParametersException("specialityId query parameter is missing")
                        val serviceCategoryId = call.parameters["serviceCategoryId"]
                            ?: throw MissingParametersException("serviceCategoryId query parameter is missing")
                        val result = specialityService.assignToServiceCategory(specialityId, serviceCategoryId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete {
                    call.requireRole(ADMIN) {
                        val specialityId = call.parameters["specialityId"]
                            ?: throw MissingParametersException("specialityId query parameter is missing")
                        val result = specialityService.unassignFromServiceCategory(specialityId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }

            route("/sub-specialities") {
                post {
                    call.requireRole(ADMIN) {
                        val specialityId = call.request.queryParameters["id"]
                            ?: throw MissingParametersException("id query parameter is missing")
                        val parts = mutableMapOf<String, String>()
                        var imageBytes: ByteArray? = null
                        var contentType = ""
                        call.receiveMultipart().forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> parts[part.name ?: ""] = part.value
                                is PartData.FileItem -> {
                                    imageBytes = part.streamProvider().readBytes()
                                    contentType = part.contentType?.toString() ?: ""
                                }

                                else -> Unit
                            }
                            part.dispose()
                        }
                        val title = parts["title"]
                            ?: return@requireRole call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("message" to "title is required")
                            )
                        val description = parts["description"]
                            ?: return@requireRole call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("message" to "description is required")
                            )
                        if (imageBytes != null) requireImageContentType(contentType)
                        val req = CreateSubSpecialityReq(
                            title = title,
                            description = description,
                            color = parts["color"]?.trim() ?: "#FFFFFF",
                        )
                        val result = specialityService.createSubSpeciality(
                            specialityId,
                            req,
                            imageBytes,
                            contentType.ifBlank { null })
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                get("all") {
                    call.requireRole(ADMIN) {
                        val specialityId = call.request.queryParameters["id"]
                            ?: throw MissingParametersException("id query parameter is missing")
                        val result = specialityService.getSubSpecialities(specialityId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }


                put {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["id"]
                            ?: throw MissingParametersException("id path parameter is missing")
                        val parts = mutableMapOf<String, String>()
                        var imageBytes: ByteArray? = null
                        var contentType = ""
                        call.receiveMultipart().forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> parts[part.name ?: ""] = part.value
                                is PartData.FileItem -> {
                                    imageBytes = part.streamProvider().readBytes()
                                    contentType = part.contentType?.toString() ?: ""
                                }

                                else -> Unit
                            }
                            part.dispose()
                        }
                        if (imageBytes != null) requireImageContentType(contentType)
                        val req = UpdateSubSpecialityReq(
                            title = parts["title"],
                            description = parts["description"],
                            color = parts["color"],
                        )
                        val result =
                            specialityService.updateSubSpeciality(id, req, imageBytes, contentType.ifBlank { null })
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                delete {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["id"]
                            ?: throw MissingParametersException("Id path parameter is missing")
                        val result = specialityService.deleteSubSpeciality(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }

        }
    }
}
