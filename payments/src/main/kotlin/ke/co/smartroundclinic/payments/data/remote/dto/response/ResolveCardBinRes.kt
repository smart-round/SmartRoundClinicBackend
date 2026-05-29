package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResolveCardBinRes(
    @SerialName("status")
    val status: Boolean,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val `data`: ResolveCardBinData,
)

@Serializable
data class ResolveCardBinData(
    @SerialName("bin")
    val bin: String,
    @SerialName("brand")
    val brand: String,
    @SerialName("sub_brand")
    val subBrand: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
    @SerialName("country_name")
    val countryName: String? = null,
    @SerialName("card_type")
    val cardType: String? = null,
    @SerialName("bank")
    val bank: String? = null,
    @SerialName("linked_bank_id")
    val linkedBankId: Int? = null,
)
