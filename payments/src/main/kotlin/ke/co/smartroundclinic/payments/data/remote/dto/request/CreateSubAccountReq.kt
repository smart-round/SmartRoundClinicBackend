package ke.co.smartroundclinic.payments.data.remote.dto.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSubAccountReq(
    @SerialName("account_number")
    val accountNumber: String,
    @SerialName("business_name")
    val businessName: String,
    @SerialName("percentage_charge")
    val percentageCharge: Double,
    @SerialName("settlement_bank")
    val settlementBank: String
)