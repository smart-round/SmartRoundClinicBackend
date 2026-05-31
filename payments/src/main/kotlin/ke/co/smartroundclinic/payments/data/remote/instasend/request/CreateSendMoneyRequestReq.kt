package ke.co.smartroundclinic.payments.data.remote.instasend.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSendMoneyRequestReq(
    @SerialName("callback_url")
    val callbackUrl: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("provider")
    val provider: String,
    @SerialName("transactions")
    val transactions: List<Transaction>
)