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
    val referralId: String? = null,
    val referredByDoctorName: String? = null,
    val referredByDoctorPicture: String? = null,
    /** Consultation price of the service tier this appointment was booked against. */
    val amount: Double? = null,
    val currency: String = "KES",
)

fun Appointment.toDetailRes(
    patientName: String?,
    patientProfilePicture: String?,
    doctorSpecialities: List<String>,
    referredByDoctorName: String? = null,
    referredByDoctorPicture: String? = null,
    amount: Double? = null,
    currency: String = "KES",
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
    referralId = referralId,
    referredByDoctorName = referredByDoctorName,
    referredByDoctorPicture = referredByDoctorPicture,
    amount = amount,
    currency = currency,
)
