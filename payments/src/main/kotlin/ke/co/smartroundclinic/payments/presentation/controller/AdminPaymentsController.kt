package ke.co.smartroundclinic.payments.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.domain.service.AdminPaymentsService

private const val ADMIN = "ADMIN"

fun Route.adminPaymentsController(service: AdminPaymentsService) {
    authenticate("auth-jwt") {
        route("/admin/payments") {

            // GET /admin/payments/overview
            // Platform-wide snapshot: payment totals, withdrawal totals, commission earned, unique doctors.
            get("overview") {
                call.requireRole(ADMIN) {
                    val result = service.getOverview()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            route("doctors") {

                // GET /admin/payments/doctors
                // Earnings, commission, withdrawals, and available balance for every doctor.
                // Sorted by totalNetEarnings descending.
                get {
                    call.requireRole(ADMIN) {
                        val result = service.getAllDoctorBreakdowns()
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /admin/payments/doctors/{doctorId}
                // Same breakdown for a single doctor.
                get("individual") {
                    call.requireRole(ADMIN) {
                        val doctorId = call.parameters["doctorId"]
                            ?: throw MissingParametersException("doctorId is required")
                        val result = service.getDoctorBreakdown(doctorId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }

            route("withdrawals") {

                // GET /admin/payments/withdrawals?page=1&size=20&status=PENDING|COMPLETED|FAILED
                // All doctor withdrawals, paginated and optionally filtered by status.
                get {
                    call.requireRole(ADMIN) {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                        val status = call.request.queryParameters["status"]
                        val result = service.getAllWithdrawals(page, size, status)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /admin/payments/withdrawals/{doctorId}
                // All withdrawals for a specific doctor (full history, no pagination).
                get("doctor") {
                    call.requireRole(ADMIN) {
                        val doctorId = call.parameters["doctorId"]
                            ?: throw MissingParametersException("doctorId is required")
                        val result = service.getWithdrawalsByDoctor(doctorId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }

            route("patients") {

                // GET /admin/payments/patients?patientId=xxx&page=1&size=20
                // Admin views full payment history for a specific patient.
                get {
                    call.requireRole(ADMIN) {
                        val patientId = call.parameters["patientId"]
                            ?: throw MissingParametersException("patientId is required")
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                        val result = service.getByPatient(patientId, page, size)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /admin/payments/patients/specific?id=xxx
                // Admin views a single payment record by ID.
                get("specific") {
                    call.requireRole(ADMIN) {
                        val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                        val result = service.getById(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }

            route("commissions") {

                // GET /admin/payments/commissions/chart?from=2026-01-01&to=2026-01-31
                // Daily earnings data points for charting. Defaults to today minus 30 days → today.
                // Each point: { date, commission, gross, disbursed, transactionCount }.
                get("chart") {
                    call.requireRole(ADMIN) {
                        val from = call.request.queryParameters["from"]
                        val to = call.request.queryParameters["to"]
                        val result = service.getEarningsChart(from, to)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /admin/payments/commissions/summary
                // Commission and earnings breakdown by period: daily, weekly, monthly, and all-time total.
                get("summary") {
                    call.requireRole(ADMIN) {
                        val result = service.getCommissionTimeSummary()
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /admin/payments/commissions?page=1&size=20
                // Paginated audit log of every platform commission entry.
                // Each entry corresponds to a doctor withdrawal and shows per-payment commission breakdown.
                get {
                    call.requireRole(ADMIN) {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                        val result = service.getAllCommissionLogs(page, size)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /admin/payments/commissions/{doctorId}
                // Full commission audit history for a specific doctor.
                get("doctor") {
                    call.requireRole(ADMIN) {
                        val doctorId = call.parameters["doctorId"]
                            ?: throw MissingParametersException("doctorId is required")
                        val result = service.getCommissionLogsByDoctor(doctorId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }
        }
    }
}
