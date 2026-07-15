package ke.co.smartroundclinic.scheduling.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class AppointmentStatsRes(
    val total: Int,
    val booked: Int,
    val confirmed: Int,
    val completed: Int,
    val cancelled: Int,
    val todayCount: Int,
    val totalRevenue: Double,
    val totalCommission: Double,
    val totalNetRevenue: Double,
)

@Serializable
data class AdminAppointmentPaymentRes(
    val paymentId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val commissionRate: Double,
    val platformFee: Double,
    val netAmount: Double,
    val paymentMethod: String?,
    val phoneNumber: String?,
    val transactionRef: String?,
    val invoiceId: String?,
    val mpesaReference: String?,
)

@Serializable
data class AdminAppointmentRes(
    val id: String,
    val doctorId: String,
    val doctorName: String,
    val patientId: String,
    val patientName: String,
    val date: String,
    val slotStart: String,
    val slotEnd: String,
    val consultationDuration: Int,
    val status: String,
    val bookedAt: String,
    val notes: String?,
    val cancellationReason: String?,
    val cancelledBy: String?,
    val updatedAt: String?,
    val payment: AdminAppointmentPaymentRes?,
)

@Serializable
data class AdminAppointmentsPageRes(
    val items: List<AdminAppointmentRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)
