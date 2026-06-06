package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetPaymentStatusRes(
    @SerialName("invoice")
    val invoice: GetPaymentStatusInvoice,
    @SerialName("meta")
    val meta: GetPaymentStatusMeta
)

@Serializable
data class GetPaymentStatusInvoice(
    @SerialName("account")
    val account: String,
    @SerialName("api_ref")
    val apiRef: String,
    @SerialName("card_info")
    val cardInfo: GetPaymentStatusCardInfo,
    @SerialName("charges")
    val charges: Double,
    @SerialName("clearing_status")
    val clearingStatus: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("failed_code")
    val failedCode: String?,
    @SerialName("failed_code_link")
    val failedCodeLink: String?,
    @SerialName("failed_reason")
    val failedReason: String?,
    @SerialName("host")
    val host: String,
    @SerialName("id")
    val id: String,
    @SerialName("invoice_id")
    val invoiceId: String,
    @SerialName("mpesa_reference")
    val mpesaReference: String,
    @SerialName("net_amount")
    val netAmount: String,
    @SerialName("provider")
    val provider: String,
    @SerialName("provider_ref")
    val providerRef: String,
    @SerialName("retry_count")
    val retryCount: Int,
    @SerialName("state")
    val state: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("value")
    val value: Int
)

@Serializable
data class GetPaymentStatusMeta(
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("customer")
    val customer: GetPaymentStatusCustomer,
    @SerialName("customer_comment")
    val customerComment: String?,
    @SerialName("id")
    val id: String,
    @SerialName("payment_link")
    val paymentLink: String?,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class GetPaymentStatusCardInfo(
    @SerialName("bin_country")
    val binCountry: String?,
    @SerialName("card_type")
    val cardType: String?
)



@Serializable
data class GetPaymentStatusCustomer(
    @SerialName("country")
    val country: String?,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("email")
    val email: String?,
    @SerialName("first_name")
    val firstName: String?,
    @SerialName("last_name")
    val lastName: String?,
    @SerialName("phone_number")
    val phoneNumber: String,
    @SerialName("provider")
    val provider: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("zipcode")
    val zipcode: String?
)