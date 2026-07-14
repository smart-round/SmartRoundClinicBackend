package ke.co.smartroundclinic.payments.presentation.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.data.remote.dto.response.WithdrawalWebhookPayload
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.presentation.dto.request.WithdrawInitiateReq
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("WithdrawalCallbackController")
private const val DOCTOR = "DOCTOR"

private val withdrawalGson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .serializeNulls()
    .create()

fun Route.doctorPaymentsController(
    paymentService: PaymentService,
    intaSendService: IntaSendService,
) {
    // Unauthenticated — IntaSend calls this server-to-server after every doctor withdrawal
    // (send-money/PesaLink) disbursement state change. Unlike collection/chargeback webhooks,
    // IntaSend's send-money event payload carries no "challenge" field to validate against
    // (see developers.intasend.com/docs/send-money-events), so this endpoint relies on
    // HTTPS + URL secrecy only.
    post("/payments/instasend/withdrawal/callback") {
        val raw = call.receiveText()

        val payload = runCatching {
            withdrawalGson.fromJson(raw, WithdrawalWebhookPayload::class.java)
        }.getOrNull()

        if (payload == null) {
            log.warn("Withdrawal webhook rejected — could not parse body")
            call.respond(HttpStatusCode.OK)
            return@post
        }

        log.info(
            "Withdrawal webhook received — trackingId=${payload.trackingId} " +
            "status=${payload.status} statusCode=${payload.statusCode} " +
            "totalAmount=${payload.totalAmount} topic=${payload.topic}"
        )

        runCatching { intaSendService.handleWithdrawalWebhook(payload) }
            .onFailure { log.error("Withdrawal webhook handler failed — ${it.message}", it) }

        call.respond(HttpStatusCode.OK)
    }

    authenticate("auth-jwt") {
        route("/doctor/payments") {

            // GET /doctor/payments?page=1
            // Wallet transaction ledger — read live from IntaSend (credits from completed
            // appointments and withdrawal debits, one unified feed), not our local PaymentEntity.
            get {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val result = intaSendService.getWalletTransactions(doctorId, page)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/payments/summary — gross earnings, platform fees, net earnings
            get("summary") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = paymentService.getDoctorSummary(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            route("withdraw") {

                // GET /doctor/payments/withdraw/balance
                // Returns net earnings, pending/completed/total withdrawn, and available balance.
                // PENDING withdrawals count against the balance — those funds are already committed.
                get("balance") {
                    call.requireRole(DOCTOR) {
                        val doctorId = call.getUserId() ?: return@requireRole
                        val result = intaSendService.getWithdrawalBalance(doctorId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // POST /doctor/payments/withdraw
                // Initiates and auto-approves a withdrawal in one step.
                // Validates minimum KES 100 and that amount <= available balance before calling IntaSend.
                post {
                    call.requireRole(DOCTOR) {
                        val doctorId = call.getUserId() ?: return@requireRole
                        val body = call.receive<WithdrawInitiateReq>()
                        val result = intaSendService.withdraw(doctorId, body)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /doctor/payments/withdraw/status?trackingId=xxx
                // Polls IntaSend for the current disbursement status.
                get("status") {
                    call.requireRole(DOCTOR) {
                        val trackingId = call.request.queryParameters["trackingId"]
                            ?: throw MissingParametersException("trackingId is required")
                        val result = intaSendService.checkWithdrawalStatus(trackingId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /doctor/payments/withdraw/history?page=1
                // Withdrawal history for the authenticated doctor, read live from IntaSend
                // (send-money/transactions scoped to their wallet), newest first.
                get("history") {
                    call.requireRole(DOCTOR) {
                        val doctorId = call.getUserId() ?: return@requireRole
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val result = intaSendService.getWithdrawalHistory(doctorId, page)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // GET /doctor/payments/withdraw/history/{id}
                // Fetch a single withdrawal record by ID. Returns 403 if it belongs to a different doctor.
                get("history/{id}") {
                    call.requireRole(DOCTOR) {
                        val doctorId = call.getUserId() ?: return@requireRole
                        val id = call.parameters["id"]
                            ?: throw MissingParametersException("id is required")
                        val result = intaSendService.getWithdrawalById(id, doctorId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }
        }
    }
}
