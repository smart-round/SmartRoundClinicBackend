package ke.co.smartroundclinic.doctor.presentation.controller

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
import ke.co.smartroundclinic.doctor.domain.service.DoctorRatingService
import ke.co.smartroundclinic.doctor.presentation.dto.request.SubmitRatingReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdateRatingReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val PATIENT = "PATIENT"

fun Route.doctorRatingController(service: DoctorRatingService) {
    authenticate("auth-jwt") {
        route("/doctor/ratings") {

            // POST /doctor/ratings
            // Patient submits a rating for a doctor.
            post {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val body = call.receive<SubmitRatingReq>()
                    val result = service.submit(body.toModel(patientId))
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /doctor/ratings?id=X
            // Patient updates their own rating.
            put {
                call.requireRole(PATIENT) {
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is required")
                    val patientId = call.getUserId() ?: return@requireRole
                    val body = call.receive<UpdateRatingReq>()
                    val result = service.update(id, patientId, body.rating, body.comment)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // DELETE /doctor/ratings?id=X
            // Patient deletes their own rating.
            delete {
                call.requireRole(PATIENT) {
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is required")
                    val patientId = call.getUserId() ?: return@requireRole
                    val result = service.delete(id, patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/ratings?id=X
            // Get a single rating by id.
            // GET /doctor/ratings?doctorId=X&page=1&size=20
            // Get all ratings for a doctor (paginated).
            get {
                val id = call.request.queryParameters["id"]
                if (id != null) {
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                } else {
                    val doctorId = call.request.queryParameters["doctorId"]
                        ?: throw MissingParametersException("doctorId query parameter is required")
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getByDoctorId(doctorId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
