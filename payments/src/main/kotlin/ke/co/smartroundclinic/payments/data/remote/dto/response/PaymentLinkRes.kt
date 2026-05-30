package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentLinkRes(
    val id: String? = null,
    val title: String,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("redirect_url") val redirectUrl: String? = null,
    val amount: Int? = null,
    @SerialName("usage_limit") val usageLimit: Int? = null,
    val currency: String? = null,
    @SerialName("mobile_tarrif") val mobileTarrif: String? = null,
    @SerialName("card_tarrif") val cardTarrif: String? = null,
    @SerialName("qrcode_file") val qrcodeFile: String? = null,
    val url: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
