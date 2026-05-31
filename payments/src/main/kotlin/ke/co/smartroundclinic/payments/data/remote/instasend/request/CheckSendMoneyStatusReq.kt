package ke.co.smartroundclinic.payments.data.remote.instasend.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckSendMoneyStatusReq(
    @SerialName("tracking_id")
    val trackingId: String,
)
