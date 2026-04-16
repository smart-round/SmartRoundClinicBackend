package ke.co.smartroundclinic.doctor.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.doctor.domain.service.LocalBankService
import ke.co.smartroundclinic.infra.plugins.MissingParametersException

fun Route.localBankController(service: LocalBankService) {
    authenticate("auth-jwt") {
        route("/doctor/banks") {

            // GET /doctor/banks?page=1&size=20
            // Paginated list of all Kenyan local banks with their branches.
            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val result = service.getAll(page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // GET /doctor/banks/search?q=Equity&page=1&size=20
            // Search banks by name (case-insensitive partial match).
            get("search") {
                val q = call.request.queryParameters["q"]
                    ?: throw MissingParametersException("q query parameter is required")
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val result = service.searchByName(q, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // GET /doctor/banks/by-code?code=01
            // Find a single bank by its bank code (exact match).
            get("by-code") {
                val code = call.request.queryParameters["code"]
                    ?: throw MissingParametersException("code query parameter is required")
                val result = service.findByCode(code)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // GET /doctor/banks/by-branch-code?branchCode=091
            // Find all banks that contain a branch with the given branch code.
            get("by-branch-code") {
                val branchCode = call.request.queryParameters["branchCode"]
                    ?: throw MissingParametersException("branchCode query parameter is required")
                val result = service.findByBranchCode(branchCode)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }
    }
}