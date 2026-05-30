package ke.co.smartroundclinic.payments.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class MpesaPaymentBody(
    val appointmentId: String,
    val phoneNumber: String,
    val amount: String,
)
