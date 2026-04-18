package ke.co.smartroundclinic.scheduling.presentation.dto.response

import ke.co.smartroundclinic.scheduling.domain.model.Appointment
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentRes(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val specialityId: String,
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

fun Appointment.toRes() = AppointmentRes(
    id = id,
    doctorId = doctorId,
    patientId = patientId,
    specialityId = specialityId,
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
