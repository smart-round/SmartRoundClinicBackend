package ke.co.smartroundclinic.patient.presentation.controller

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
import ke.co.smartroundclinic.patient.domain.service.PatientRatingService
import ke.co.smartroundclinic.patient.presentation.dto.request.SubmitPatientRatingReq
import ke.co.smartroundclinic.patient.presentation.dto.request.UpdatePatientRatingReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"

fun Route.patientRatingController(service: PatientRatingService) {
    authenticate("auth-jwt") {
        route("/patient/ratings") {

            // POST /patient/ratings
            // Doctor submits a rating for a patient.
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<SubmitPatientRatingReq>()
                    val result = service.submit(body.toModel(doctorId))
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /patient/ratings?id=X
            // Doctor updates their own rating.
            put {
                call.requireRole(DOCTOR) {
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is required")
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<UpdatePatientRatingReq>()
                    val result = service.update(id, doctorId, body.rating, body.comment)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // DELETE /patient/ratings?id=X
            // Doctor deletes their own rating.
            delete {
                call.requireRole(DOCTOR) {
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is required")
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.delete(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /patient/ratings?id=X
            // Get a single rating by id.
            // GET /patient/ratings?patientId=X&page=1&size=20
            // Get all ratings for a patient (paginated).
            get {
                val id = call.request.queryParameters["id"]
                if (id != null) {
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                } else {
                    val patientId = call.request.queryParameters["patientId"]
                        ?: throw MissingParametersException("patientId query parameter is required")
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getByPatientId(patientId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
