package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntaSendInvoice(
    @SerialName("invoice_id") val invoiceId: String? = null,
    val state: String? = null,
    val provider: String? = null,
    val value: String? = null,
    val account: String? = null,
    @SerialName("api_ref") val apiRef: String? = null,
    @SerialName("mpesa_reference") val mpesaReference: String? = null,
    val currency: String? = null,
    val charges: String? = null,
    @SerialName("net_amount") val netAmount: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class IntaSendCustomer(
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val email: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
)

@Serializable
data class PaymentSessionRes(
    val id: String? = null,
    val invoice: IntaSendInvoice? = null,
    val customer: IntaSendCustomer? = null,
    @SerialName("payment_link") val paymentLink: PaymentLinkRes? = null,
    @SerialName("customer_comment") val customerComment: String? = null,
    val refundable: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
