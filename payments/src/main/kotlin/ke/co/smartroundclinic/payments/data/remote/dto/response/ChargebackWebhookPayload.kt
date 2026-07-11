package ke.co.smartroundclinic.payments.data.remote.dto.response

/** IntaSend fires this whenever a chargeback's status changes (used here for refunds). */
data class ChargebackWebhookPayload(
    val chargebackId: String? = null,
    val amount: String? = null,
    val reason: String? = null,
    val status: String? = null,
    val challenge: String? = null,
)
