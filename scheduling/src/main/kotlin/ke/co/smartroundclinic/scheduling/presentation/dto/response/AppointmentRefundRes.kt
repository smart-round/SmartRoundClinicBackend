package ke.co.smartroundclinic.scheduling.presentation.dto.response

import ke.co.smartroundclinic.scheduling.data.lookup.RefundDoc
import kotlinx.serialization.Serializable

/** Status mirrors [ke.co.smartroundclinic.scheduling.data.lookup.RefundDoc]: PENDING -> COMPLETED | FAILED. */
@Serializable
data class AppointmentRefundRes(
    val id: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val reason: String?,
    val createdAt: String,
    val updatedAt: String?,
)

fun RefundDoc.toAppointmentRefundRes() = AppointmentRefundRes(
    id = id,
    amount = amount,
    currency = currency,
    status = status,
    reason = reason,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
