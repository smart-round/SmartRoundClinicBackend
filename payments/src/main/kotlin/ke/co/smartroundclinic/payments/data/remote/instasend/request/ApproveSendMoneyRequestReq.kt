package ke.co.smartroundclinic.payments.data.remote.instasend.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApproveSendMoneyRequestReq(
    @SerialName("tracking_id")
    val trackingId: String,
    @SerialName("transactions")
    val transactions: List<ApproveSendMoneyTransaction>,
    @SerialName("wallet")
    val wallet: ApproveSendMoneyWallet
)

@Serializable
data class ApproveSendMoneyTransaction(
    @SerialName("account")
    val account: String,
    @SerialName("amount")
    val amount: String,
    @SerialName("bank_code")
    val bankCode: String
)

@Serializable
data class ApproveSendMoneyWallet(
    @SerialName("available_balance")
    val availableBalance: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("label")
    val label: String,
    @SerialName("wallet_type")
    val walletType: String
)