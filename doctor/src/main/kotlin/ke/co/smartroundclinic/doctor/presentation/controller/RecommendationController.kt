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

            // GET /doctor/recommendations?page=1&size=20
            // General ranked list: doctors ordered by (speciality demand × doctor score).
            // Doctors in high-booking specialities surface first; within a speciality the
            // best-rated / most-booked doctors rank higher.

            // GET /doctor/recommendations?specializationId=X&page=1&size=20
            // Speciality-specific ranking: pure doctor score (rating 50%, bookings 35%, reviews 15%).
            get {
                val specializationId = call.request.queryParameters["specializationId"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val result = service.getRecommendations(specializationId, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }
    }
}
