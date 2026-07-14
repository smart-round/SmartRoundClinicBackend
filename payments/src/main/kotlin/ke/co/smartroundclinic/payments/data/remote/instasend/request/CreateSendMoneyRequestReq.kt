package ke.co.smartroundclinic.payments.data.remote.instasend.request


import ke.co.smartroundclinic.infra.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSendMoneyRequestReq(
    @SerialName("callback_url")
    val callbackUrl: String = AppConfig.intaSend.callBackWithdrawalUrl,
    @SerialName("currency")
    val currency: String = "KES",
    @SerialName("provider")
    val provider: String = "PESALINK",
    @SerialName("transactions")
    val transactions: List<CreateSendMoneyTransaction>,
    @SerialName("wallet_id")
    val walletId: String? = null,
    @SerialName("requires_approval")
    val requiresApproval: String = "NO",
)

@Serializable
data class CreateSendMoneyTransaction(
    @SerialName("account")
    val account: String,
    @SerialName("amount")
    val amount: String,
    @SerialName("bank_code")
    val bankCode: String,
    @SerialName("id_number")
    val idNumber: String,
    @SerialName("name")
    val name: String,
    @SerialName("narrative")
    val narrative: String? = null,
){
    fun addIdNumber(idNumber: String): CreateSendMoneyTransaction  = this.copy(idNumber = idNumber)
}