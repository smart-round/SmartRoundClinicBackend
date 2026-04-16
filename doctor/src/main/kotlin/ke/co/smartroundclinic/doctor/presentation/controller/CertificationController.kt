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
import ke.co.smartroundclinic.doctor.domain.service.CertificationService
import ke.co.smartroundclinic.doctor.presentation.dto.request.AddCertificationReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdateCertificationReq
import ke.co.smartroundclinic.doctor.presentation.dto.response.CertificationRes
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireImageContentType
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"

fun Route.certificationController(service: CertificationService) {
    authenticate("auth-jwt") {
        route("/doctor/certifications") {

            // POST /doctor/certifications
            // Upload a new certification.
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    var imageBytes: ByteArray? = null
                    var contentType = ""
                    var body:AddCertificationReq? = null

                    call.receiveMultipart().forEachPart { part ->
                        if (part is PartData.FileItem) {
                            imageBytes = part.streamProvider().readBytes()
                            contentType = part.contentType?.toString() ?: ""
                        }
                        part.dispose()

                        if (part is PartData.FormItem) {
                            body = AddCertificationReq(
                                certificationName = part.value,
                                certificationDate = part.value
                            )
                        }
                    }

                    val bytes = imageBytes
                        ?: return@requireRole call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("message" to "No image file provided")
                        )
                    body ?: throw MissingParametersException("Missing items certificateName or certificateDate  is required")
                    requireImageContentType(contentType)

                    val result = service.add(contentType = contentType, licence = bytes, body.toModel(doctorId))
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/certifications
            // List all my certifications.
            get("all"){
                call.requireRole(DOCTOR) {
                    val id = call.parameters["id"]
                    val doctorId = (call.getUserId() ?: id) ?: return@requireRole
                    val result = service.getAll(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/certifications/{id}
            // Get a single certification.
            get {
                call.requireRole(DOCTOR) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.getById(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }


            // DELETE /doctor/certifications/{id}
            // Delete a certification.
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