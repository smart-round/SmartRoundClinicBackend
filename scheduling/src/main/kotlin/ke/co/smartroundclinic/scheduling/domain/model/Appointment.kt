package ke.co.smartroundclinic.scheduling.domain.model

data class Appointment(
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
)
