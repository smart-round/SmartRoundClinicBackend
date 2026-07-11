package ke.co.smartroundclinic.scheduling.presentation.dto.response

import ke.co.smartroundclinic.scheduling.domain.model.Appointment
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentRes(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val doctorName: String? = null,
    val doctorProfilePicture: String? = null,
    val doctorSpeciality: String? = null,
    val serviceTierId: String = "",
    val consultationDuration: Int = 0,
    val date: String,
    val slotStart: String,
    val slotEnd: String,
    val status: String,
    val bookedAt: String,
    val notes: String?,
    val cancellationReason: String?,
    val cancelledBy: String?,
    val updatedAt: String?,
    val refund: AppointmentRefundRes? = null,
)

@Serializable
data class AppointmentAdminRes(
    val id: String,
    val doctorId: String,
    val doctorName: String?,
    val doctorSpecialities: List<String>,
    val patientId: String,
    val serviceTierId: String = "",
    val consultationDuration: Int = 0,
    val date: String,
    val slotStart: String,
    val slotEnd: String,
    val status: String,
    val bookedAt: String,
    val cancellationReason: String?,
    val cancelledBy: String?,
    val updatedAt: String?,
)

fun Appointment.toAdminRes(doctorName: String?, doctorSpecialities: List<String>) = AppointmentAdminRes(
    id = id,
    doctorId = doctorId,
    doctorName = doctorName,
    doctorSpecialities = doctorSpecialities,
    patientId = patientId,
    serviceTierId = serviceTierId,
    consultationDuration = consultationDuration,
    date = date,
    slotStart = slotStart,
    slotEnd = slotEnd,
    status = status,
    bookedAt = bookedAt,
    cancellationReason = cancellationReason,
    cancelledBy = cancelledBy,
    updatedAt = updatedAt,
)

/**
 * Minimal shape for GET /scheduling/appointments/next. Deliberately excludes name/picture
 * enrichment — the caller already has the counterpart's display info from the chat thread it's
 * asking about; this only carries what's needed to decide whether/when to offer a video call.
 */
@Serializable
data class NextAppointmentRes(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val date: String,
    val slotStart: String,
    val slotEnd: String,
    val status: String,
)

fun Appointment.toNextAppointmentRes() = NextAppointmentRes(
    id = id,
    doctorId = doctorId,
    patientId = patientId,
    date = date,
    slotStart = slotStart,
    slotEnd = slotEnd,
    status = status,
)

fun Appointment.toRes(
    doctorName: String? = null,
    doctorProfilePicture: String? = null,
    doctorSpeciality: String? = null,
    refund: AppointmentRefundRes? = null,
) = AppointmentRes(
    id = id,
    doctorId = doctorId,
    patientId = patientId,
    doctorName = doctorName,
    doctorProfilePicture = doctorProfilePicture,
    doctorSpeciality = doctorSpeciality,
    serviceTierId = serviceTierId,
    consultationDuration = consultationDuration,
    date = date,
    slotStart = slotStart,
    slotEnd = slotEnd,
    status = status,
    bookedAt = bookedAt,
    notes = notes,
    cancellationReason = cancellationReason,
    cancelledBy = cancelledBy,
    updatedAt = updatedAt,
    refund = refund,
)
