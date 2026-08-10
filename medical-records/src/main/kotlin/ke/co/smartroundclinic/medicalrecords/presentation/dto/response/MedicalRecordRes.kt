package ke.co.smartroundclinic.medicalrecords.presentation.dto.response

import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecord
import ke.co.smartroundclinic.medicalrecords.domain.model.PrescriptionItem
import kotlinx.serialization.Serializable

@Serializable
data class PrescriptionItemRes(
    val drug: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String? = null,
)

@Serializable
data class MedicalRecordRes(
    val id: String,
    val appointmentId: String,
    val consultationId: String? = null,
    val doctorId: String,
    val patientId: String,
    val diagnosis: String? = null,
    val prescription: List<PrescriptionItemRes> = emptyList(),
    val summary: String? = null,
    val referralNote: String? = null,
    val labRequests: List<String> = emptyList(),
    val additionalNotes: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val doctorName: String? = null,
    /**
     * Set only on the copy embedded in a chat card, naming the [ke.co.smartroundclinic
     * .medicalrecords.domain.model.MedicalRecordField] entries this revision changed. Always empty
     * on the REST responses, where the caller is reading the record rather than being told about a
     * change to it.
     */
    val editedFields: List<String> = emptyList(),
)

fun PrescriptionItem.toRes() = PrescriptionItemRes(drug, dosage, frequency, duration, instructions)

fun MedicalRecord.toRes() = MedicalRecordRes(
    id = id,
    appointmentId = appointmentId,
    consultationId = consultationId,
    doctorId = doctorId,
    patientId = patientId,
    diagnosis = diagnosis,
    prescription = prescription.map { it.toRes() },
    summary = summary,
    referralNote = referralNote,
    labRequests = labRequests,
    additionalNotes = additionalNotes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
