package ke.co.smartroundclinic.payments.data.remote.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class PaystackErrorRes(
    val status: Boolean,
    val message: String,
)
