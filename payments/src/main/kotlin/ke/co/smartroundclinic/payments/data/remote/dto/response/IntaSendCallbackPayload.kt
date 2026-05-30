package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntaSendCallbackPayload(
    @SerialName("invoice_id") val invoiceId: String? = null,
    val state: String? = null,
    val value: String? = null,
    val account: String? = null,
    @SerialName("api_ref") val apiRef: String? = null,
    @SerialName("mpesa_reference") val mpesaReference: String? = null,
    val currency: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val charges: String? = null,
    @SerialName("net_amount") val netAmount: String? = null,
)
