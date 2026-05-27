package ke.co.smartroundclinic.payments.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class PaymentRes(
    val id: String,
    val appointmentId: String,
    val patientId: String,
    val doctorId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paymentMethod: String?,
    val transactionRef: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class PaymentsPageRes(
    val items: List<PaymentRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)
