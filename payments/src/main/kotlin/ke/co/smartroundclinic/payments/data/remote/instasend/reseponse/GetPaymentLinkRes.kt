package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetPaymentLink(
    @SerialName("amount")
    val amount: Int,
    @SerialName("card_tarrif")
    val cardTarrif: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("id")
    val id: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("mobile_tarrif")
    val mobileTarrif: String,
    @SerialName("qrcode_file")
    val qrcodeFile: String,
    @SerialName("redirect_url")
    val redirectUrl: Any?,
    @SerialName("title")
    val title: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("url")
    val url: String,
    @SerialName("usage_limit")
    val usageLimit: Int
)