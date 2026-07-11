package ke.co.smartroundclinic.payments.data.remote.instasend.reseponse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Exact response schema isn't published by IntaSend — fields are nullable/defaulted defensively. */
@Serializable
data class CreateChargebackRequestRes(
    @SerialName("chargeback_id")
    val chargebackId: String? = null,
    @SerialName("amount")
    val amount: String? = null,
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
