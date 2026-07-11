package ke.co.smartroundclinic.payments.data.remote.instasend.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /chargebacks/ — IntaSend's native refund: reverses the original collection transaction
 *  by invoice_id instead of initiating a fresh outbound disbursement, so it doesn't carry
 *  B2C-style payout charges. */
@Serializable
data class CreateChargebackRequestReq(
    @SerialName("invoice_id")
    val invoiceId: String,
    @SerialName("amount")
    val amount: String,
    @SerialName("reason")
    val reason: String,
)
