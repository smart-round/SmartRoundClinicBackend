package ke.co.smartroundclinic.doctor.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.doctor.domain.service.PractitionerLicenceService
import ke.co.smartroundclinic.doctor.presentation.dto.request.AddCertificationReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.AddLicenceReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdateLicenceReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireImageContentType
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"
private const val ADMIN = "ADMIN"

fun Route.practitionerLicenceController(service: PractitionerLicenceService) {
    authenticate("auth-jwt") {
        route("/doctor/licence") {
            // POST /doctor/licences
            // Upload a new licence document.
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    var imageBytes: ByteArray? = null
                    var contentType = ""
                    var body:AddLicenceReq? = null

                    call.receiveMultipart().forEachPart { part ->
                        if (part is PartData.FileItem) {
                            imageBytes = part.streamProvider().readBytes()
                            contentType = part.contentType?.toString() ?: ""
                        }
                        part.dispose()

                        if (part is PartData.FormItem) {
                            body = AddLicenceReq(
                                licenceName = part.value,
                            )
                        }
                    }

                    val bytes = imageBytes
                        ?: return@requireRole call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("message" to "No image file provided")
                        )
                    body ?: throw MissingParametersException("Missing items licenceName  is required")
                    requireImageContentType(contentType)
                    val result = service.add(contentType = contentType, licence = bytes, body.toModel(doctorId))
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/licences
            // List all my licence documents.
            get("all") {
                call.requireRole(DOCTOR,  ADMIN ) {
                    val id = call.parameters["id"]
                    val doctorId = (call.getUserId() ?: id) ?: return@requireRole
                    val result = service.getAll(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
            // GET /doctor/licences/{id}
            // Get a single licence document.
            get {
                call.requireRole(ADMIN,DOCTOR) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("licence id path parameter is required")
                    val getDocId = call.parameters["doctorId"]
                    val doctorId = (getDocId ?: call.getUserId()) ?: return@requireRole

                    val result = service.getById(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
            // DELETE /doctor/licences/{id}
            // Delete a licence document.
            delete {
                call.requireRole(DOCTOR) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.delete(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}