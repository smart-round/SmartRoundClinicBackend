package ke.co.smartroundclinic.payments.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MpesaStkPushReq(
    val amount: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("api_ref") val apiRef: String? = null,
    @SerialName("wallet_id") val walletId: String? = null,
    @SerialName("mobile_tarrif") val mobileTarrif: String? = null,
)
