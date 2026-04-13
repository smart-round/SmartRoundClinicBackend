package ke.co.smartroundclinic.admin.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.admin.domain.model.DoctorLicenceStatus
import ke.co.smartroundclinic.admin.domain.model.KmpdcRegisterType
import ke.co.smartroundclinic.admin.domain.service.KmpdcService
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"

fun Route.kmpdcController(kmpdcService: KmpdcService) {
    authenticate("auth-jwt") {
        route("/admin/kmpdc") {

            // POST /admin/kmpdc/refresh  — ADMIN only
            // Scrapes all 10 KMPDC registers and upserts into MongoDB.
            post("refresh") {
                call.requireRole(ADMIN) {
                    val result = kmpdcService.refresh()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /admin/kmpdc/practitioners?registerType=MEDICAL_MASTER&status=ACTIVE&page=1&size=50
            // Any authenticated role. Returns paginated list of practitioners.
            get("practitioners") {
                val registerTypeRaw = call.request.queryParameters["registerType"]
                val statusRaw = call.request.queryParameters["status"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50

                val registerType = registerTypeRaw?.let {
                    KmpdcRegisterType.fromOrNull(it)
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("message" to "Invalid registerType. Valid values: ${KmpdcRegisterType.entries.map { t -> t.name }}")
                        )
                }

                val status = statusRaw?.let {
                    runCatching { DoctorLicenceStatus.from(it) }
                        .getOrElse {
                            return@get call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("message" to "Invalid status. Valid values: ACTIVE, INACTIVE, UNKNOWN")
                            )
                        }
                }

                val result = kmpdcService.getPractitioners(registerType, status, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // GET /admin/kmpdc/practitioners/search?q=John+Odhiambo&page=1&size=20
            // Any authenticated role. Fuzzy name search — most similar results first.
            get("practitioners/search") {
                val q = call.request.queryParameters["q"]
                    ?: throw MissingParametersException("q query parameter is required")
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val result = kmpdcService.searchByName(q, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // GET /admin/kmpdc/practitioners/lookup?regNumber=M123
            // Any authenticated role. Look up a single practitioner by registration number.
            get("practitioners/lookup") {
                val regNumber = call.request.queryParameters["regNumber"]
                    ?: throw MissingParametersException("regNumber query parameter is required")
                val result = kmpdcService.findByRegNumber(regNumber)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }
    }
}