package ke.co.smartroundclinic.doctor.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.doctor.domain.service.PractitionerProfileService
import ke.co.smartroundclinic.doctor.presentation.dto.request.CreatePractitionerProfileReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdatePractitionerProfileReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"

fun Route.practitionerProfileController(service: PractitionerProfileService) {
    authenticate("auth-jwt") {

        // ── Doctor-only: manage own profile ──────────────────────────────────
        route("/doctor/profile") {

            // POST /doctor/profile
            // Create the doctor's profile (once per account).
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<CreatePractitionerProfileReq>()
                    val result = service.create(body.toModel(doctorId))
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /doctor/profile
            // Partially update the doctor's own profile.
            put {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<UpdatePractitionerProfileReq>()
                    val result = service.update(doctorId, body.toDomain())
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/profile/me
            // Fetch the authenticated doctor's own profile.
            get("me") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.getMyProfile(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }

        // ── Any authenticated role: browse practitioners ──────────────────────
        route("/doctor/profiles") {

            // GET /doctor/profiles?page=1&size=20
            // Paginated list of all practitioners.
            get {
                val id = call.request.queryParameters["id"]
                if (id != null) {
                    val result = service.getByIdWithSpecializations(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                } else {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getAll(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}