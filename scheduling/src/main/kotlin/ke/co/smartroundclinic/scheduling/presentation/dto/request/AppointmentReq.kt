package ke.co.smartroundclinic.scheduling.presentation.dto.request

import ke.co.smartroundclinic.scheduling.domain.model.Appointment
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class BookAppointmentReq(
    val doctorId: String,
    val specialityId: String,
    val date: String,
    val slotStart: String,
    val notes: String? = null,
) {
    fun toModel(patientId: String, slotEnd: String) = Appointment(
        id = ObjectId().toString(),
        doctorId = doctorId,
        patientId = patientId,
        specialityId = specialityId,
        date = date,
        slotStart = slotStart,
        slotEnd = slotEnd,
        status = "BOOKED",
        bookedAt = Clock.System.now().toString(),
        notes = notes,
    )
}

@Serializable
data class CancelAppointmentReq(val reason: String? = null)
