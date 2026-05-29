package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateAccountRes(
    @SerialName("status")
    val status: Boolean,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val `data`: ValidateAccountData,
)

@Serializable
data class ValidateAccountData(
    @SerialName("verified")
    val verified: Boolean,
    @SerialName("verificationMessage")
    val verificationMessage: String,
)
