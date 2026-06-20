package ke.co.smartroundclinic.medicalrecords.domain.model

data class MedicalRecord(
    val id: String,
    val appointmentId: String,
    val consultationId: String?,
    val doctorId: String,
    val patientId: String,
    val diagnosis: String?,
    val prescription: List<PrescriptionItem>,
    val summary: String?,
    val referralNote: String?,
    val labRequests: List<String>,
    val additionalNotes: String?,
    val createdAt: String,
    val updatedAt: String?,
)

data class PrescriptionItem(
    val drug: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String?,
)
