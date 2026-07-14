package ke.co.smartroundclinic.payments.data.remote.instasend.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateWalletReq(
    @SerialName("currency")
    val currency: String = "KES",
    @SerialName("label")
    val label: String,
    @SerialName("wallet_type")
    val walletType: String = "WORKING",
    @SerialName("can_disburse")
    val canDisburse: Boolean = false,
)
