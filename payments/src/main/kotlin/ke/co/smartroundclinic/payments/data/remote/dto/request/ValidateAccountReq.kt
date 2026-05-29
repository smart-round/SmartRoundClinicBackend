package ke.co.smartroundclinic.payments.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateAccountReq(
    @SerialName("account_name")
    val accountName: String,
    @SerialName("account_number")
    val accountNumber: String,
    @SerialName("account_type")
    val accountType: String,
    @SerialName("bank_code")
    val bankCode: String,
    @SerialName("country_code")
    val countryCode: String,
    @SerialName("document_type")
    val documentType: String,
    @SerialName("document_number")
    val documentNumber: String? = null,
)
