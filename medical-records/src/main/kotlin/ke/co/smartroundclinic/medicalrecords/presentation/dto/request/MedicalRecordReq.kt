package ke.co.smartroundclinic.medicalrecords.presentation.dto.request

import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecord
import ke.co.smartroundclinic.medicalrecords.domain.model.PrescriptionItem
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class PrescriptionItemReq(
    val drug: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val instructions: String? = null,
)

@Serializable
data class SaveMedicalRecordReq(
    val appointmentId: String? = null,
    val consultationId: String? = null,
    val patientId: String? = null,
    val diagnosis: String? = null,
    val prescription: List<PrescriptionItemReq>? = null,
    val summary: String? = null,
    val referralNote: String? = null,
    val labRequests: List<String>? = null,
    val additionalNotes: String? = null,
) {
    fun toModel(doctorId: String) = MedicalRecord(
        id = ObjectId().toString(),
        appointmentId = appointmentId ?: "",
        consultationId = consultationId,
        doctorId = doctorId,
        patientId = patientId ?: "",
        diagnosis = diagnosis,
        prescription = prescription?.map { PrescriptionItem(it.drug ?: "", it.dosage ?: "", it.frequency ?: "", it.duration ?: "", it.instructions) } ?: emptyList(),
        summary = summary,
        referralNote = referralNote,
        labRequests = labRequests ?: emptyList(),
        additionalNotes = additionalNotes,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}
