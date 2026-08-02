package ke.co.smartroundclinic.doctor.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.doctor.domain.service.RecommendationService

fun Route.recommendationController(service: RecommendationService) {
    authenticate("auth-jwt") {
        route("/doctor/recommendations") {
            get {
                val specializationId = call.request.queryParameters["specializationId"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val excludeDoctorId = call.request.queryParameters["excludeDoctorId"]
                val result = service.getRecommendations(specializationId, page, size, excludeDoctorId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // GET /doctor/recommendations/{doctorId} — single-doctor profile lookup, e.g. for
            // viewing a doctor's profile from a doctor-chat thread.
            get("{doctorId}") {
                val doctorId = call.parameters["doctorId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val result = service.getById(doctorId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

    }
}
