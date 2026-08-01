package ke.co.smartroundclinic.referral.domain.model

data class Referral(
    val id: String,
    val sourceAppointmentId: String,
    val referringDoctorId: String,
    val referringDoctorName: String?,
    val referringDoctorPicture: String?,
    val patientId: String,
    val receivingDoctorId: String,
    val reason: String,
    val status: String,
    val resultingAppointmentId: String?,
    val createdAt: String,
    val respondedAt: String?,
)
