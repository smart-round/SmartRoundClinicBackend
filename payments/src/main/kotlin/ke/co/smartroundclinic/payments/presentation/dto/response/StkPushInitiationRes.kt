package ke.co.smartroundclinic.payments.presentation.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StkPushInitiationRes(
    @SerialName("invoice_id") val invoiceId: String,
    @SerialName("transaction_ref") val transactionRef: String,
    val state: String,
    val amount: Double,
    val currency: String,
)
