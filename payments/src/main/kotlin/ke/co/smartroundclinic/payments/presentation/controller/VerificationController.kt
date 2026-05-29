package ke.co.smartroundclinic.payments.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.payments.domain.service.VerificationService

fun Route.verificationController(service: VerificationService) {
    authenticate("auth-jwt") {
        route("/payments") {

            // GET /payments/card-bin/{bin} — Resolve card BIN information (any authenticated role)
            get("card-bin/{bin}") {
                val bin = call.parameters["bin"]
                    ?: throw MissingParametersException("bin path parameter is required")
                val result = service.resolveCardBin(bin)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }
    }
}
