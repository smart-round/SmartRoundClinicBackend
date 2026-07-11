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

            // POST /doctor/compliance/corrections/confirm
            // A rejected doctor confirms they've made the requested changes — creates a new
            // PENDING correction record, resolved later when an admin approves/rejects again.
            post("corrections/confirm") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.confirmCorrection(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }

        // ── Admin: review and approve/reject doctors ──────────────────────────
        route("/admin/compliance") {

            // GET /admin/compliance?page=1&size=20&status=APPROVED|PENDING|REJECTED&name=John
            // Admin lists all compliance records with optional status and name filters.
            get {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val status = call.request.queryParameters["status"]
                    val name = call.request.queryParameters["name"]
                    val result = service.getAll(page, size, status, name)
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

            // PUT /admin/compliance/{id}/monetize
            // Admin enables monetization for an approved doctor.
            put("monetize") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val result = service.setMonetization(id, true)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /admin/compliance/{id}/demonetize
            // Admin disables monetization — doctor becomes invisible to patients.
            put("demonetize") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val result = service.setMonetization(id, false)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // ── Admin: correction confirmations ─────────────────────────────
            route("corrections") {

                // GET /admin/compliance/corrections?page=1&size=20&status=PENDING
                // Admin queue — most recently submitted correction confirmations across all doctors.
                get {
                    call.requireRole(ADMIN) {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                        val status = call.request.queryParameters["status"]
                        val result = service.getLatestCorrections(page, size, status)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /admin/compliance/corrections/history?doctorId=&page=1&size=20
                // Admin — full correction history for a specific doctor (can span multiple rejections).
                get("history") {
                    call.requireRole(ADMIN) {
                        val doctorId = call.request.queryParameters["doctorId"]
                            ?: throw MissingParametersException("doctorId query parameter is required")
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                        val result = service.getCorrectionHistory(doctorId, page, size)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }
        }
    }
}
