package ke.co.smartroundclinic.payments.data.remote.instasend.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentLinkReq(
    @SerialName("amount")
    val amount: Int,
    @SerialName("card_tarrif")
    val cardTarrif: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("id")
    val id: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("mobile_tarrif")
    val mobileTarrif: String,
    @SerialName("redirect_url")
    val redirectUrl: String,
    @SerialName("title")
    val title: String,
    @SerialName("usage_limit")
    val usageLimit: Int
)