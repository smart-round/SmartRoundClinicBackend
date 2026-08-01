
package ke.co.smartroundclinic.scheduling.data.entity

import ke.co.smartroundclinic.scheduling.domain.model.Appointment
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentEntity(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val serviceTierId: String = "",
    val consultationDuration: Int = 0,
    val date: String,
    val slotStart: String,
    val slotEnd: String,
    val status: String,
    val bookedAt: String,
    val notes: String? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val updatedAt: String? = null,
    val referralId: String? = null,
) {
    fun toModel() = Appointment(
        id = id,
        doctorId = doctorId,
        patientId = patientId,
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
        referralId = referralId,
    )
}

fun Appointment.toEntity() = AppointmentEntity(
    id = id,
    doctorId = doctorId,
    patientId = patientId,
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
    referralId = referralId,
)
