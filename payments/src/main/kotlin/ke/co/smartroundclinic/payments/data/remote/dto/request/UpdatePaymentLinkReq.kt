package ke.co.smartroundclinic.payments.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePaymentLinkReq(
    val title: String,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("redirect_url") val redirectUrl: String? = null,
    val amount: Double? = null,
    @SerialName("usage_limit") val usageLimit: Int? = null,
    val currency: String? = null,
    @SerialName("mobile_tarrif") val mobileTarrif: String? = null,
    @SerialName("card_tarrif") val cardTarrif: String? = null,
)
