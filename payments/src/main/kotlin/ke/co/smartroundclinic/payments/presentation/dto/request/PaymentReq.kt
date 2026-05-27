package ke.co.smartroundclinic.payments.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class InitiatePaymentReq(
    val appointmentId: String,
    val doctorId: String,
    val amount: Double,
    val currency: String = "KES",
    val paymentMethod: String? = null,
    val notes: String? = null,
)

@Serializable
data class UpdatePaymentStatusReq(
    val status: String,
    val transactionRef: String? = null,
)
