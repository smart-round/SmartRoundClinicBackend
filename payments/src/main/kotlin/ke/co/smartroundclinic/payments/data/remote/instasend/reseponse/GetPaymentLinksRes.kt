package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetPaymentLinksRes(
    @SerialName("count")
    val count: Int,
    @SerialName("next")
    val next: Any?,
    @SerialName("previous")
    val previous: Any?,
    @SerialName("results")
    val results: List<Result>
)