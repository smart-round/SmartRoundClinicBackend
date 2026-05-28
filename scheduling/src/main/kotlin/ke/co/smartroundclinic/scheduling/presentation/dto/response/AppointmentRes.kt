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

fun Appointment.toRes(
    doctorName: String? = null,
    doctorProfilePicture: String? = null,
    doctorSpeciality: String? = null,
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
)
