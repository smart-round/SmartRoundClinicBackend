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
                val result = service.getRecommendations(specializationId, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

    }
}
