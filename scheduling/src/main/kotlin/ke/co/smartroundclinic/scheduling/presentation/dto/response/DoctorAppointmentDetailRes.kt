package ke.co.smartroundclinic.scheduling.presentation.dto.response

import ke.co.smartroundclinic.scheduling.domain.model.Appointment

data class DoctorAppointmentDetailRes(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val patientName: String?,
    val patientProfilePicture: String?,
    val doctorSpecialities: List<String>,
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

fun Appointment.toDetailRes(
    patientName: String?,
    patientProfilePicture: String?,
    doctorSpecialities: List<String>,
) = DoctorAppointmentDetailRes(
    id = id,
    doctorId = doctorId,
    patientId = patientId,
    patientName = patientName,
    patientProfilePicture = patientProfilePicture,
    doctorSpecialities = doctorSpecialities,
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
