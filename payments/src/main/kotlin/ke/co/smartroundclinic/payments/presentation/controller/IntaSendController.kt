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
import ke.co.smartroundclinic.payments.presentation.dto.request.UpdatePaymentLinkBody
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("IntaSendController")

// ── HTML page builders ────────────────────────────────────────────────────────

/** Shared CSS + Google Fonts loaded once via a base template. */
private fun htmlPage(title: String, accentBg: String, content: String) = """
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
      --brand:        #2563EB;
      --brand-dark:   #1D4ED8;
      --brand-light:  #EFF6FF;
      --brand-mid:    #BFDBFE;
      --success:      #059669;
      --success-bg:   #ECFDF5;
      --success-ring: #6EE7B7;
      --error:        #DC2626;
      --error-bg:     #FEF2F2;
      --error-ring:   #FCA5A5;
      --warn:         #D97706;
      --warn-bg:      #FFFBEB;
      --warn-ring:    #FDE68A;
      --text:         #0F172A;
      --muted:        #64748B;
      --subtle:       #94A3B8;
      --surface:      #FFFFFF;
      --radius:       20px;
    }

    body {
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
      background: $accentBg;
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px 20px;
      -webkit-font-smoothing: antialiased;
    }

    /* ── Header bar ── */
    .header {
      width: 100%;
      max-width: 460px;
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 20px;
    }
    .logo-mark {
      width: 36px; height: 36px;
      background: var(--brand);
      border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .logo-mark svg { width: 20px; height: 20px; }
    .brand-name {
      font-size: 15px; font-weight: 700; color: var(--brand-dark);
      letter-spacing: -0.2px;
    }
    .brand-sub { font-size: 11px; color: var(--muted); font-weight: 400; }

    /* ── Card ── */
    .card {
      background: var(--surface);
      border-radius: var(--radius);
      box-shadow: 0 1px 3px rgba(0,0,0,.06), 0 8px 32px rgba(37,99,235,.08);
      max-width: 460px;
      width: 100%;
      padding: 40px 36px 36px;
      text-align: center;
    }

    /* ── Status icon circle ── */
    .icon-wrap {
      width: 80px; height: 80px;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 28px;
      animation: pop .4s cubic-bezier(.34,1.56,.64,1) both;
    }
    @keyframes pop {
      from { transform: scale(.5); opacity: 0; }
      to   { transform: scale(1);  opacity: 1; }
    }
    .icon-wrap svg { width: 40px; height: 40px; }

    /* ── Typography ── */
    .status-title {
      font-size: 22px; font-weight: 800;
      color: var(--text);
      letter-spacing: -0.5px;
      margin-bottom: 10px;
    }
    .status-msg {
      font-size: 14px; line-height: 1.7;
      color: var(--muted);
      margin-bottom: 24px;
    }

    /* ── Amount pill ── */
    .amount-pill {
      display: inline-flex; align-items: baseline; gap: 4px;
      background: var(--brand-light);
      border: 1px solid var(--brand-mid);
      border-radius: 12px;
      padding: 10px 20px;
      margin-bottom: 20px;
    }
    .amount-currency { font-size: 15px; font-weight: 600; color: var(--brand); }
    .amount-value    { font-size: 32px; font-weight: 800; color: var(--brand-dark); letter-spacing: -1px; }

    /* ── Invoice badge ── */
    .ref-badge {
      display: flex; align-items: center; justify-content: center; gap: 6px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 10px;
      padding: 10px 14px;
      font-size: 12px; color: var(--muted);
      margin-bottom: 28px;
      word-break: break-all;
    }
    .ref-badge svg { width: 14px; height: 14px; flex-shrink: 0; color: var(--subtle); }

    /* ── Reason box (error) ── */
    .reason-box {
      background: var(--error-bg);
      border: 1px solid var(--error-ring);
      border-radius: 10px;
      padding: 12px 16px;
      font-size: 13px; color: var(--error);
      margin-bottom: 24px;
      text-align: left;
    }

    /* ── Divider ── */
    .divider { border: none; border-top: 1px solid #F1F5F9; margin: 0 0 24px; }

    /* ── Close-tab instruction ── */
    .close-section {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 14px;
      padding: 18px 20px;
      margin-bottom: 24px;
    }
    .close-section p {
      font-size: 13px; color: var(--muted); line-height: 1.6; margin-bottom: 14px;
    }
    .close-section p strong { color: var(--text); }
    .close-btn {
      display: inline-flex; align-items: center; gap: 8px;
      background: var(--brand);
      color: #fff;
      border: none; border-radius: 10px;
      padding: 11px 22px;
      font-size: 14px; font-weight: 600;
      cursor: pointer;
      font-family: inherit;
      transition: background .15s;
      width: 100%;
      justify-content: center;
    }
    .close-btn:hover { background: var(--brand-dark); }
    .close-btn svg { width: 16px; height: 16px; }

    /* ── Footer ── */
    .footer {
      font-size: 11px; color: var(--subtle);
      display: flex; align-items: center; justify-content: center; gap: 6px;
    }
    .footer svg { width: 12px; height: 12px; }

    /* ── Spinner (pending) ── */
    .spinner {
      width: 44px; height: 44px;
      border: 3px solid var(--warn-ring);
      border-top-color: var(--warn);
      border-radius: 50%;
      animation: spin .8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  </style>
</head>
<body>
  <div class="header">
    <div class="logo-mark">
      <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
      </svg>
    </div>
    <div>
      <div class="brand-name">Smart Round Clinic</div>
      <div class="brand-sub">Secure Payment</div>
    </div>
  </div>

  <div class="card">
    $content
  </div>

  <script>
    function closeTab() {
      window.close();
      // Fallback: show manual instruction if window.close() is blocked
      setTimeout(function() {
        var btn = document.getElementById('close-btn');
        if (btn) btn.textContent = 'Please close this tab manually';
      }, 300);
    }
  </script>
</body>
</html>
""".trimIndent()

private fun closeTabSection(msg: String) = """
    <hr class="divider"/>
    <div class="close-section">
      <p>$msg</p>
      <button class="close-btn" id="close-btn" onclick="closeTab()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <path d="M18 6L6 18M6 6l12 12"/>
        </svg>
        Close Tab &amp; Return to App
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
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 10h8M8 14h5"/></svg>
      Invoice&nbsp;<strong>$invoiceId</strong>
    </div>""" else ""

    val close = closeTabSection("<strong>Your payment was successful.</strong> Close this tab and return to the Smart Round Clinic app to complete your appointment booking.")

    return htmlPage("Payment Successful", "linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%)", """
    <div class="icon-wrap" style="background:#DCFCE7">
      <svg viewBox="0 0 24 24" fill="none" stroke="#16A34A" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/>
        <path d="M8 12l3 3 5-5"/>
      </svg>
    </div>
    <div class="status-title">Payment Successful</div>
    <div class="status-msg">Your payment has been received and confirmed.<br/>You're all set!</div>
    $amountBlock
    $refBlock
    $close
    <div class="footer">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
      Secured by IntaSend &bull; Smart Round Clinic
    </div>
""".trimIndent())
}

private fun paymentFailedHtml(reason: String): String {
    val close = closeTabSection("<strong>Something went wrong.</strong> Close this tab and return to the Smart Round Clinic app to try again.")

    return htmlPage("Payment Failed", "linear-gradient(135deg, #FEF2F2 0%, #FEE2E2 100%)", """
    <div class="icon-wrap" style="background:#FEE2E2">
      <svg viewBox="0 0 24 24" fill="none" stroke="#DC2626" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/>
        <path d="M15 9l-6 6M9 9l6 6"/>
      </svg>
    </div>
    <div class="status-title">Payment Failed</div>
    <div class="status-msg">We couldn't process your payment. Please review the details below.</div>
    <div class="reason-box">$reason</div>
    $close
    <div class="footer">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
      Secured by IntaSend &bull; Smart Round Clinic
    </div>
""".trimIndent())
}

private fun paymentPendingHtml(invoiceId: String?): String {
    val refBlock = if (invoiceId != null) """
    <div class="ref-badge">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 10h8M8 14h5"/></svg>
      Invoice&nbsp;<strong>$invoiceId</strong>
    </div>""" else ""

    val close = closeTabSection("<strong>Your payment is being processed.</strong> Close this tab and return to the Smart Round Clinic app. We'll notify you once it's confirmed.")

    return htmlPage("Payment Processing", "linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%)", """
    <div class="icon-wrap" style="background:#FEF3C7; animation: none;">
      <div class="spinner"></div>
    </div>
    <div class="status-title">Processing Payment</div>
    <div class="status-msg">Your payment is on its way. This usually takes less than a minute.</div>
    $refBlock
    $close
    <div class="footer">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
      Secured by IntaSend &bull; Smart Round Clinic
    </div>
""".trimIndent())
}

private fun webhookSuccessHtml(state: String?, invoiceId: String?, value: String?, currency: String?) = """
<!DOCTYPE html><html><head><meta charset="UTF-8"/><title>Webhook OK</title>
<style>body{font-family:monospace;background:#f0fdf4;padding:32px;color:#166534}
.box{background:#fff;border:1px solid #bbf7d0;border-radius:8px;padding:20px;max-width:500px}
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
                "COMPLETE" -> paymentSuccessHtml(invoiceId, value, currency)
                "FAILED"   -> paymentFailedHtml(failed ?: "Your payment could not be processed. Please try again.")
                else       -> paymentPendingHtml(invoiceId)
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
