package ke.co.smartroundclinic.medicalrecords.presentation.dto.request

import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecord
import ke.co.smartroundclinic.medicalrecords.domain.model.PrescriptionItem
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class PrescriptionItemReq(
    val drug: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String? = null,
)

@Serializable
data class SaveMedicalRecordReq(
    val appointmentId: String,
    val consultationId: String? = null,
    val patientId: String,
    val diagnosis: String? = null,
    val prescription: List<PrescriptionItemReq> = emptyList(),
    val summary: String? = null,
    val referralNote: String? = null,
    val labRequests: List<String> = emptyList(),
    val additionalNotes: String? = null,
) {
    fun toModel(doctorId: String) = MedicalRecord(
        id = ObjectId().toString(),
        appointmentId = appointmentId,
        consultationId = consultationId,
        doctorId = doctorId,
        patientId = patientId,
        diagnosis = diagnosis,
        prescription = prescription.map { PrescriptionItem(it.drug, it.dosage, it.frequency, it.duration, it.instructions) },
        summary = summary,
        referralNote = referralNote,
        labRequests = labRequests,
        additionalNotes = additionalNotes,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}
