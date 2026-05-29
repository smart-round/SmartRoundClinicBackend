package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResolveAccountRes(
    @SerialName("status")
    val status: Boolean,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val `data`: ResolveAccountData,
)

@Serializable
data class ResolveAccountData(
    @SerialName("account_number")
    val accountNumber: String,
    @SerialName("account_name")
    val accountName: String,
    @SerialName("bank_id")
    val bankId: Int,
)
