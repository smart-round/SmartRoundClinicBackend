package ke.co.smartroundclinic.payments.data.remote.instasend.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateMpesaB2CRequestReq(
    @SerialName("callback_url")
    val callbackUrl: String,
    @SerialName("currency")
    val currency: String = "KES",
    @SerialName("provider")
    val provider: String = "MPESA-B2C",
    @SerialName("transactions")
    val transactions: List<CreateMpesaB2CTransaction>,
)

@Serializable
data class CreateMpesaB2CTransaction(
    @SerialName("account")
    val account: String,
    @SerialName("amount")
    val amount: String,
    @SerialName("name")
    val name: String,
    @SerialName("narrative")
    val narrative: String? = null,
)
