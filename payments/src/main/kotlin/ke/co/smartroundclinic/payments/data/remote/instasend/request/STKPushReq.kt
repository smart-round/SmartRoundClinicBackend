package ke.co.smartroundclinic.payments.data.remote.instasend.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class STKPushReq(
    @SerialName("amount")
    val amount: String,
    @SerialName("api_ref")
    val apiRef: String,
    @SerialName("mobile_tarrif")
    val mobileTarrif: STKPushMobileTarrif = STKPushMobileTarrif.CUSTOMER_PAYS,
    @SerialName("phone_number")
    val phoneNumber: String,
    @SerialName("signature")
    val signature: String
)

enum class STKPushMobileTarrif(val str: String) {
    CUSTOMER_PAYS("CUSTOMER-PAYS"),
}