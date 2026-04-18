package ke.co.smartroundclinic.scheduling.data.entity

import ke.co.smartroundclinic.scheduling.domain.model.Appointment
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentEntity(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val specialityId: String,
    val date: String,
    val slotStart: String,
    val slotEnd: String,
    val status: String,
    val bookedAt: String,
    val notes: String? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val updatedAt: String? = null,
) {
    fun toModel() = Appointment(
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
}

fun Appointment.toEntity() = AppointmentEntity(
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
