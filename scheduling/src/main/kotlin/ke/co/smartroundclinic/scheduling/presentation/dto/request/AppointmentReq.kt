package ke.co.smartroundclinic.scheduling.presentation.dto.request

import ke.co.smartroundclinic.scheduling.domain.model.Appointment
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

data class BookAppointmentReq(
    val doctorId: String,
    val date: String,
    val slotStart: String,
    val transactionRef: String,
    val notes: String? = null,
) {
    fun toModel(patientId: String, slotEnd: String, serviceTierId: String, consultationDuration: Int) = Appointment(
        id = ObjectId().toString(),
        doctorId = doctorId,
        patientId = patientId,
        serviceTierId = serviceTierId,
        consultationDuration = consultationDuration,
        date = date,
        slotStart = slotStart,
        slotEnd = slotEnd,
        status = "BOOKED",
        bookedAt = Clock.System.now().toString(),
        notes = notes,
    )
}

data class CancelAppointmentReq(val reason: String? = null)
