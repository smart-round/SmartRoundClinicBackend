package ke.co.smartroundclinic.payments.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.domain.service.SubAccountService
import ke.co.smartroundclinic.payments.presentation.dto.request.DoctorUpdateSubAccountReq
import ke.co.smartroundclinic.payments.presentation.dto.request.toPaystackReq

private const val DOCTOR = "DOCTOR"
private const val ADMIN = "ADMIN"

fun Route.subAccountController(service: SubAccountService) {
    authenticate("auth-jwt") {
        route("/doctor/subaccounts") {

            // POST /doctor/subaccounts — Doctor creates a Paystack subaccount (if none exists)
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.createMine(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/subaccounts — Doctor fetches their own Paystack subaccount
            get {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.getMine(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /doctor/subaccounts — Doctor updates their own Paystack subaccount
            put {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<DoctorUpdateSubAccountReq>()
                    val result = service.updateMine(doctorId, body.toPaystackReq())
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/subaccounts/all?perPage=50&page=1&from=&to= — Admin lists all subaccounts
            get("all") {
                call.requireRole(ADMIN) {
                    val perPage = call.request.queryParameters["perPage"]?.toIntOrNull() ?: 50
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val from = call.request.queryParameters["from"]
                    val to = call.request.queryParameters["to"]
                    val result = service.list(perPage, page, from, to)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/subaccounts/{idOrCode} — Admin fetches a specific subaccount
            get("account") {
                call.requireRole(ADMIN) {
                    val idOrCode = call.parameters["idOrCode"]
                        ?: throw MissingParametersException("idOrCode path parameter is required")
                    val result = service.getByIdOrCode(idOrCode)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
