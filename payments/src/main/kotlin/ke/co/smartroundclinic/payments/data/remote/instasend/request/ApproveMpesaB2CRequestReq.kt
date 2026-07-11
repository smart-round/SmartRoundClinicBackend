package ke.co.smartroundclinic.payments.data.remote.instasend.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApproveMpesaB2CRequestReq(
    @SerialName("tracking_id")
    val trackingId: String,
    @SerialName("transactions")
    val transactions: List<ApproveMpesaB2CTransaction>,
    @SerialName("wallet")
    val wallet: ApproveMpesaB2CWallet? = null,
)

@Serializable
data class ApproveMpesaB2CTransaction(
    @SerialName("account")
    val account: String,
    @SerialName("amount")
    val amount: String,
)

@Serializable
data class ApproveMpesaB2CWallet(
    @SerialName("available_balance")
    val availableBalance: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("label")
    val label: String,
    @SerialName("wallet_type")
    val walletType: String,
)
