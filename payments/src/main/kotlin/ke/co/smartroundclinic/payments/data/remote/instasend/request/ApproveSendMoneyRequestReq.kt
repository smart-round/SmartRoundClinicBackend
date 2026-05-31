package ke.co.smartroundclinic.payments.data.remote.instasend.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApproveSendMoneyRequestReq(
    @SerialName("tracking_id")
    val trackingId: String,
    @SerialName("transactions")
    val transactions: List<Transaction>,
    @SerialName("wallet")
    val wallet: Wallet
)