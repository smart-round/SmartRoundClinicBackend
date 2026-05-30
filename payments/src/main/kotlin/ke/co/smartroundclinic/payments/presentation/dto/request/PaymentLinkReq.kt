package ke.co.smartroundclinic.payments.presentation.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentLinkBody(
    val title: String,
    @SerialName("is_active") val isActive: Boolean? = null,
    val amount: Int? = null,
    @SerialName("usage_limit") val usageLimit: Int? = null,
    val currency: String? = null,
    @SerialName("mobile_tarrif") val mobileTarrif: String? = null,
    @SerialName("card_tarrif") val cardTarrif: String? = null,
)

@Serializable
data class UpdatePaymentLinkBody(
    val title: String,
    @SerialName("is_active") val isActive: Boolean? = null,
    val amount: Int? = null,
    @SerialName("usage_limit") val usageLimit: Int? = null,
    val currency: String? = null,
    @SerialName("mobile_tarrif") val mobileTarrif: String? = null,
    @SerialName("card_tarrif") val cardTarrif: String? = null,
)
