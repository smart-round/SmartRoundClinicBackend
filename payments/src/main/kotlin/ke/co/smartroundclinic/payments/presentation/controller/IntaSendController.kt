package ke.co.smartroundclinic.payments.presentation.controller

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendCallbackPayload
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import ke.co.smartroundclinic.payments.presentation.dto.request.CreateAppointmentPaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.CreatePaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.CreatePreBookingPaymentLinkBody
import ke.co.smartroundclinic.payments.presentation.dto.request.StkPushAppointmentBody
import ke.co.smartroundclinic.payments.presentation.dto.request.StkPushPreBookingBody
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentLinkBody
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("IntaSendController")

// ── HTML page builders ────────────────────────────────────────────────────────
// Brand palette (Smart Round Clinic):
//   Primary   #E84E1C  · Primary70 #FF8A65  · Primary80 #FFB59B
//   Primary90 #FFDBCE  · Primary95 #FFEDE8  · Primary99 #FFF8F6

private const val LOGO_URL = "https://admin.smartroundclinic.co.ke/logo-md.png"

private fun htmlPage(title: String, content: String) = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>$title — Smart Round Clinic</title>
  <link rel="preconnect" href="https://fonts.googleapis.com"/>
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

    :root {
      --brand:        #E84E1C;
      --brand-dark:   #C73D12;
      --brand-70:     #FF8A65;
      --brand-80:     #FFB59B;
      --brand-90:     #FFDBCE;
      --brand-95:     #FFEDE8;
      --brand-99:     #FFF8F6;
      --success:      #059669;
      --success-bg:   #ECFDF5;
      --success-ring: #6EE7B7;
      --error:        #DC2626;
      --error-bg:     #FEF2F2;
      --error-ring:   #FCA5A5;
      --text:         #1A0A06;
      --muted:        #6B4C42;
      --subtle:       #A07060;
      --border:       #F0D5CC;
      --surface:      #FFFFFF;
      --radius:       20px;
    }

    body {
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
      background: var(--brand-99);
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 28px 20px;
      -webkit-font-smoothing: antialiased;
    }

    /* ── Header ── */
    .header {
      width: 100%;
      max-width: 460px;
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
      padding: 0 4px;
    }
    .logo { height: 36px; width: auto; display: block; }
    .secure-badge {
      margin-left: auto;
      display: flex; align-items: center; gap: 5px;
      font-size: 11px; font-weight: 500; color: var(--subtle);
      background: var(--brand-95); border: 1px solid var(--brand-90);
      border-radius: 20px; padding: 4px 10px;
    }
    .secure-badge svg { width: 11px; height: 11px; }

    /* ── Card ── */
    .card {
      background: var(--surface);
      border-radius: var(--radius);
      box-shadow: 0 2px 4px rgba(232,78,28,.06), 0 12px 40px rgba(232,78,28,.10);
      border: 1px solid var(--brand-95);
      max-width: 460px;
      width: 100%;
      overflow: hidden;
    }

    /* ── Card top stripe ── */
    .card-stripe {
      height: 5px;
      background: linear-gradient(90deg, var(--brand) 0%, var(--brand-70) 100%);
    }

    .card-body { padding: 40px 36px 36px; text-align: center; }

    /* ── Icon circle ── */
    .icon-wrap {
      width: 84px; height: 84px;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 28px;
      animation: pop .45s cubic-bezier(.34,1.56,.64,1) both;
    }
    @keyframes pop {
      from { transform: scale(.4); opacity: 0; }
      to   { transform: scale(1);  opacity: 1; }
    }
    .icon-wrap svg { width: 42px; height: 42px; }

    /* ── Typography ── */
    .status-title {
      font-size: 23px; font-weight: 800;
      color: var(--text);
      letter-spacing: -0.5px;
      margin-bottom: 10px;
    }
    .status-msg {
      font-size: 14px; line-height: 1.75;
      color: var(--muted);
      margin-bottom: 28px;
    }

    /* ── Amount pill ── */
    .amount-pill {
      display: inline-flex; align-items: baseline; gap: 5px;
      background: var(--brand-95);
      border: 1.5px solid var(--brand-90);
      border-radius: 14px;
      padding: 12px 24px;
      margin-bottom: 20px;
    }
    .amount-currency { font-size: 16px; font-weight: 700; color: var(--brand); }
    .amount-value    { font-size: 36px; font-weight: 800; color: var(--brand-dark); letter-spacing: -1.5px; }

    /* ── Invoice badge ── */
    .ref-badge {
      display: flex; align-items: center; justify-content: center; gap: 7px;
      background: var(--brand-99);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 10px 16px;
      font-size: 12px; color: var(--muted);
      margin-bottom: 28px;
      word-break: break-all;
    }
    .ref-badge svg { width: 14px; height: 14px; flex-shrink: 0; color: var(--brand-70); }

    /* ── Reason box ── */
    .reason-box {
      background: var(--error-bg);
      border: 1px solid var(--error-ring);
      border-radius: 10px;
      padding: 12px 16px;
      font-size: 13px; color: var(--error);
      margin-bottom: 28px;
      text-align: left;
      line-height: 1.6;
    }

    /* ── Divider ── */
    .divider { border: none; border-top: 1px solid var(--brand-95); margin: 0 0 24px; }

    /* ── Close-tab block ── */
    .close-section {
      background: var(--brand-99);
      border: 1px solid var(--brand-90);
      border-radius: 14px;
      padding: 20px;
      margin-bottom: 24px;
    }
    .close-section p {
      font-size: 13px; color: var(--muted); line-height: 1.65; margin-bottom: 16px;
    }
    .close-section p strong { color: var(--text); }
    .close-btn {
      display: flex; align-items: center; justify-content: center; gap: 8px;
      background: var(--brand);
      color: #fff;
      border: none; border-radius: 12px;
      padding: 14px 24px;
      font-size: 15px; font-weight: 700;
      cursor: pointer;
      font-family: inherit;
      width: 100%;
      transition: background .15s, transform .1s;
      letter-spacing: -0.1px;
    }
    .close-btn:hover  { background: var(--brand-dark); }
    .close-btn:active { transform: scale(.98); }
    .close-btn svg { width: 17px; height: 17px; }

    /* ── Footer ── */
    .footer {
      font-size: 11px; color: var(--subtle);
      display: flex; align-items: center; justify-content: center; gap: 6px;
      padding-top: 4px;
    }
    .footer svg { width: 12px; height: 12px; color: var(--brand-70); }
  </style>
</head>
<body>
  <div class="header">
    <img src="$LOGO_URL" alt="Smart Round Clinic" class="logo"
         onerror="this.style.display='none';document.getElementById('brand-fallback').style.display='flex'"/>
    <div id="brand-fallback" style="display:none;align-items:center;gap:8px">
      <div style="width:34px;height:34px;background:var(--brand);border-radius:9px;display:flex;align-items:center;justify-content:center">
        <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
        </svg>
      </div>
      <span style="font-size:15px;font-weight:700;color:var(--brand-dark)">Smart Round Clinic</span>
    </div>
    <div class="secure-badge">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
      Secure Payment
    </div>
  </div>

  <div class="card">
    <div class="card-stripe"></div>
    <div class="card-body">
      $content
    </div>
  </div>

  <script>
    function closeTab() {
      window.close();
      setTimeout(function() {
        var btn = document.getElementById('close-btn');
        if (btn) { btn.textContent = 'Please close this tab manually and return to the app'; }
      }, 400);
    }
  </script>
</body>
</html>
""".trimIndent()

private fun closeTabBlock(msg: String) = """
    <hr class="divider"/>
    <div class="close-section">
      <p>$msg</p>
      <button class="close-btn" id="close-btn" onclick="closeTab()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <polyline points="9 18 3 12 9 6"/><line x1="3" y1="12" x2="21" y2="12"/>
        </svg>
        Close Tab &amp; Go Back to App
      </button>
    </div>
""".trimIndent()

private fun paymentSuccessHtml(invoiceId: String?, amount: String?, currency: String): String {
    val amountBlock = if (amount != null) """
    <div class="amount-pill">
      <span class="amount-currency">$currency</span>
      <span class="amount-value">$amount</span>
    </div>""" else ""

    val refBlock = if (invoiceId != null) """
    <div class="ref-badge">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
        <rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 10h8M8 14h5"/>
      </svg>
      Invoice&nbsp;<strong>$invoiceId</strong>
    </div>""" else ""

    val close = closeTabBlock("Your payment was confirmed. <strong>Close this tab and go back to the Smart Round Clinic app</strong> to complete your appointment booking.")

    return htmlPage("Payment Successful", """
    <div class="icon-wrap" style="background:#DCFCE7">
      <svg viewBox="0 0 24 24" fill="none" stroke="#16A34A" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-5"/>
      </svg>
    </div>
    <div class="status-title">Payment Successful</div>
    <div class="status-msg">Your payment has been received and confirmed.<br/>You're all set!</div>
    $amountBlock
    $refBlock
    $close
    <div class="footer">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
        <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
      </svg>
      Secured by IntaSend &bull; Smart Round Clinic
    </div>
""".trimIndent())
}

private fun paymentFailedHtml(reason: String): String {
    val close = closeTabBlock("Something went wrong with your payment. <strong>Close this tab and go back to the Smart Round Clinic app</strong> to try again.")

    return htmlPage("Payment Failed", """
    <div class="icon-wrap" style="background:#FEE2E2">
      <svg viewBox="0 0 24 24" fill="none" stroke="#DC2626" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/><path d="M15 9l-6 6M9 9l6 6"/>
      </svg>
    </div>
    <div class="status-title">Payment Failed</div>
    <div class="status-msg">We couldn't complete your payment. Please review the error below.</div>
    <div class="reason-box">$reason</div>
    $close
    <div class="footer">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
        <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
      </svg>
      Secured by IntaSend &bull; Smart Round Clinic
    </div>
""".trimIndent())
}

private fun webhookSuccessHtml(state: String?, invoiceId: String?, value: String?, currency: String?) = """
<!DOCTYPE html><html><head><meta charset="UTF-8"/><title>Webhook OK</title>
<style>body{font-family:monospace;background:#FFF8F6;padding:32px;color:#C73D12}
.box{background:#fff;border:1px solid #FFB59B;border-radius:8px;padding:20px;max-width:500px}
h2{margin-bottom:12px}p{color:#374151;font-size:13px;line-height:1.8}</style></head>
<body><div class="box">
  <h2>&#10003; Webhook processed</h2>
  <p>state &nbsp;&nbsp;&nbsp;&nbsp;= ${state ?: "—"}<br/>
     invoiceId = ${invoiceId ?: "—"}<br/>
     value &nbsp;&nbsp;&nbsp;&nbsp;= ${value ?: "—"} ${currency ?: ""}</p>
</div></body></html>
""".trimIndent()

private fun webhookErrorHtml(reason: String) = """
<!DOCTYPE html><html><head><meta charset="UTF-8"/><title>Webhook Error</title>
<style>body{font-family:monospace;background:#fef2f2;padding:32px;color:#991b1b}
.box{background:#fff;border:1px solid #fca5a5;border-radius:8px;padding:20px;max-width:500px}
h2{margin-bottom:12px}p{color:#374151;font-size:13px}</style></head>
<body><div class="box">
  <h2>&#10007; Webhook error</h2>
  <p>$reason</p>
</div></body></html>
""".trimIndent()

// Gson with snake_case → camelCase mapping to deserialize IntaSend's webhook body
private val webhookGson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .serializeNulls()
    .create()

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.intaSendController(service: IntaSendService, webhookChallenge: String) {

    // ── Unauthenticated callback endpoints ────────────────────────────────────

    route("/payments/intasend/callback") {

        // POST — IntaSend webhook: server-to-server notification after payment.
        // Read body ONCE as raw text, then parse with Gson (server uses Gson, not kotlinx).
        // Always responds 200 so IntaSend does not retry; body is HTML for observability.
        post {
            val raw = call.receiveText()
            log.info("IntaSend webhook received: $raw")

            val payload = runCatching {
                webhookGson.fromJson(raw, IntaSendCallbackPayload::class.java)
            }.getOrNull()

            if (payload == null) {
                log.warn("IntaSend webhook could not be parsed — raw=$raw")
                call.respondText(webhookErrorHtml("Could not parse webhook payload"), ContentType.Text.Html, HttpStatusCode.OK)
                return@post
            }

            if (payload.challenge != webhookChallenge) {
                log.warn("IntaSend webhook rejected — challenge mismatch (received=${payload.challenge})")
                call.respondText(webhookErrorHtml("Invalid webhook challenge"), ContentType.Text.Html, HttpStatusCode.OK)
                return@post
            }

            val handlerError = runCatching { service.handleWebhook(payload) }
                .exceptionOrNull()
                .also { if (it != null) log.error("Webhook handler failed — ${it.message}", it) }

            val html = if (handlerError == null)
                webhookSuccessHtml(payload.state, payload.invoiceId, payload.value, payload.currency)
            else
                webhookErrorHtml(handlerError.message ?: "Internal error while processing webhook")

            call.respondText(html, ContentType.Text.Html, HttpStatusCode.OK)
        }

        // GET — browser redirect after payment; IntaSend appends state and invoice params.
        get {
            val params = call.request.queryParameters
            val state     = params["state"]?.uppercase()
            val invoiceId = params["invoice_id"] ?: params["invoiceId"]
            val value     = params["value"]
            val currency  = params["currency"] ?: "KES"
            val failed    = params["failed_reason"] ?: params["failedReason"]

            log.info("IntaSend redirect callback — state=$state invoiceId=$invoiceId value=$value failed=$failed")

            val html = when (state) {
                "FAILED" -> paymentFailedHtml(failed ?: "Your payment could not be processed. Please try again.")
                else     -> paymentSuccessHtml(invoiceId, value, currency)
            }
            call.respondText(html, ContentType.Text.Html, HttpStatusCode.OK)
        }
    }



    // ── Authenticated payment-link management ─────────────────────────────────

    authenticate("auth-jwt") {

        // POST /payments/intasend/appointments
        // Patient creates a payment link for their appointment.
        // Title, ID, amount, and currency are all auto-filled.
        // Response includes data.url — the IntaSend payment page the patient uses.
        post("/payments/intasend/appointments") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val body = call.receive<CreateAppointmentPaymentLinkBody>()
                val result = service.createForAppointment(body, patientId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // POST /payments/intasend/pre-booking?rebooking=true|false
        // Patient creates a payment link BEFORE booking an appointment.
        // Amount is resolved from the doctor's service tier.
        // For rebooking (?rebooking=true), also supply previousAppointmentId in the body to
        // load the follow-up fee; the previous appointment must be COMPLETED and in the same month.
        // The returned transactionRef must be supplied when calling POST /scheduling/appointments.
        post("/payments/intasend/pre-booking") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val isRebooking = call.request.queryParameters["rebooking"]?.lowercase() == "true"
                val body = call.receive<CreatePreBookingPaymentLinkBody>()
                val result = service.createPreBookingLink(
                    doctorId = body.doctorId,
                    patientId = patientId,
                    isRebooking = isRebooking,
                    previousAppointmentId = body.previousAppointmentId,
                )
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // POST /payments/intasend/appointments/stk-push
        // Patient triggers an M-Pesa STK push for an existing appointment.
        // Returns invoice_id (for polling) and transaction_ref (for booking) in the response.
        post("/payments/intasend/appointments/stk-push") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val body = call.receive<StkPushAppointmentBody>()
                val result = service.stkPushForAppointment(body.appointmentId, body.phoneNumber, patientId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // POST /payments/intasend/pre-booking/stk-push?rebooking=true|false
        // Patient triggers an M-Pesa STK push BEFORE booking an appointment.
        // Returns invoice_id (for polling) and transaction_ref (for booking) in the response.
        post("/payments/intasend/pre-booking/stk-push") {
            call.requireRole(PATIENT) {
                val patientId = call.getUserId() ?: return@requireRole
                val isRebooking = call.request.queryParameters["rebooking"]?.lowercase() == "true"
                val body = call.receive<StkPushPreBookingBody>()
                val result = service.stkPushPreBooking(
                    doctorId = body.doctorId,
                    patientId = patientId,
                    phoneNumber = body.phoneNumber,
                    isRebooking = isRebooking,
                    previousAppointmentId = body.previousAppointmentId,
                )
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // GET /payments/intasend/stk-push/status?invoiceId=X
        // Poll the status of an STK push payment by invoice ID.
        get("/payments/intasend/stk-push/status") {
            val invoiceId = call.request.queryParameters["invoiceId"]
                ?: throw MissingParametersException("invoiceId is required")
            val result = service.getStkPushStatus(invoiceId)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        route("/payments/intasend/links") {

            // POST /payments/intasend/links — admin creates a generic payment link
            post {
                call.requireRole(ADMIN) {
                    val body = call.receive<CreatePaymentLinkBody>()
                    val result = service.create(body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/intasend/links?page=1
            get {
                call.requireRole(ADMIN) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val result = service.list(page)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /payments/intasend/links/{id}
            get("{id}") {
                call.requireRole(ADMIN, DOCTOR) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = service.get(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /payments/intasend/links/{id}
            put("{id}") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val body = call.receive<UpdatePaymentLinkBody>()
                    val result = service.update(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
