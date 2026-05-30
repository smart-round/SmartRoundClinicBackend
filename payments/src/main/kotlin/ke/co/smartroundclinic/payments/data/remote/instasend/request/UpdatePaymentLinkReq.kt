package ke.co.smartroundclinic.payments.data.remote.instasend.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePaymentLinkReq(
    @SerialName("amount")
    val amount: Int,
    @SerialName("card_tarrif")
    val cardTarrif: UpdateCardTarrif = UpdateCardTarrif.CUSTOMER_PAYS,
    @SerialName("currency")
    val currency: String,
    @SerialName("id")
    val id: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("mobile_tarrif")
    val mobileTarrif: UpdateMobileTarrif = UpdateMobileTarrif.CUSTOMER_PAYS,
    @SerialName("redirect_url")
    val redirectUrl: String,
    @SerialName("title")
    val title: String,
    @SerialName("usage_limit")
    val usageLimit: Int
)


enum class UpdateMobileTarrif(val value: String) {
    CUSTOMER_PAYS("CUSTOMER-PAYS"),
    BUSINESS_PAYS("BUSINESS-PAYS"),
}

enum class UpdateCardTarrif(val value: String) {
    CUSTOMER_PAYS("CUSTOMER-PAYS"),
    BUSINESS_PAYS("BUSINESS-PAYS"),
}