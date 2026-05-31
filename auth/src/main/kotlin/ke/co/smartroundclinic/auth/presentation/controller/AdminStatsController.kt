package ke.co.smartroundclinic.auth.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.auth.domain.usecase.GetUserStatsUseCase

private const val ADMIN = "ADMIN"

fun Route.adminStatsController(getUserStatsUseCase: GetUserStatsUseCase) {
    authenticate("auth-jwt") {
        route("/auth/admin") {

            // GET /auth/admin/stats
            // Platform user statistics: role counts, account/verification status breakdown,
            // doctor approval rate, new user growth by period, gender breakdown, and
            // the 10 most recently registered users.
            get("stats") {
                call.requireRole(ADMIN) {
                    val result = getUserStatsUseCase()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
