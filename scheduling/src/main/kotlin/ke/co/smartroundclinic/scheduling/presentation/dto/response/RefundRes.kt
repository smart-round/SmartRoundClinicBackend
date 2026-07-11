package ke.co.smartroundclinic.scheduling.presentation.dto.response

import ke.co.smartroundclinic.scheduling.data.lookup.RefundDoc
import kotlinx.serialization.Serializable

@Serializable
data class RefundRes(
    val id: String,
    val appointmentId: String,
    val doctorId: String,
    val doctorName: String,
    val patientId: String,
    val patientName: String,
    val paymentId: String,
    val amount: Double,
    val currency: String,
    val reason: String?,
    val cancelledBy: String,
    val cancelledByRole: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class RefundsPageRes(
    val items: List<RefundRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun RefundDoc.toRes(names: Map<String, String> = emptyMap()) = RefundRes(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    doctorName = names[doctorId] ?: "Unknown Doctor",
    patientId = patientId,
    patientName = names[patientId] ?: "Unknown Patient",
    paymentId = paymentId,
    amount = amount,
    currency = currency,
    reason = reason,
    cancelledBy = cancelledBy,
    cancelledByRole = cancelledByRole,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
