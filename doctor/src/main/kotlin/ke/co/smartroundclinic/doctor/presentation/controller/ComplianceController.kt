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
import ke.co.smartroundclinic.doctor.domain.service.ComplianceService
import ke.co.smartroundclinic.doctor.presentation.dto.request.RejectComplianceReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"
private const val ADMIN = "ADMIN"

fun Route.complianceController(service: ComplianceService) {
    authenticate("auth-jwt") {

        // ── Doctor: submit for review & check own status ──────────────────────
        route("/doctor/compliance") {

            // POST /doctor/compliance
            // Doctor submits their profile for compliance review.
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.submit(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/compliance/status
            // Doctor checks their own compliance/approval status.
            get("status") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.getMyStatus(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }

        // ── Admin: review and approve/reject doctors ──────────────────────────
        route("/admin/compliance") {

            // GET /admin/compliance?page=1&size=20
            // Admin lists all compliance records.
            get {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getAll(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /admin/compliance/{id}
            // Admin gets a single compliance record by id.
            get("doctor") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /admin/compliance/{id}/approve
            // Admin approves a doctor.
            put("approve") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val adminId = call.getUserId() ?: return@requireRole
                    val result = service.approve(id, adminId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /admin/compliance/{id}/reject
            // Admin rejects a doctor with a reason.
            put("reject") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val adminId = call.getUserId() ?: return@requireRole
                    val body = call.receive<RejectComplianceReq>()
                    val result = service.reject(id, adminId, body.reason)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
