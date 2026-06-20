package ke.co.smartroundclinic.medicalrecords.data.entity

import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecord
import ke.co.smartroundclinic.medicalrecords.domain.model.PrescriptionItem
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class MedicalRecordEntity(
    val id: String = ObjectId().toString(),
    val appointmentId: String,
    val consultationId: String? = null,
    val doctorId: String,
    val patientId: String,
    val diagnosis: String? = null,
    val prescription: List<PrescriptionItemEntity> = emptyList(),
    val summary: String? = null,
    val referralNote: String? = null,
    val labRequests: List<String> = emptyList(),
    val additionalNotes: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = MedicalRecord(
        id = id,
        appointmentId = appointmentId,
        consultationId = consultationId,
        doctorId = doctorId,
        patientId = patientId,
        diagnosis = diagnosis,
        prescription = prescription.map { it.toModel() },
        summary = summary,
        referralNote = referralNote,
        labRequests = labRequests,
        additionalNotes = additionalNotes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Serializable
data class PrescriptionItemEntity(
    val drug: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String? = null,
) {
    fun toModel() = PrescriptionItem(drug, dosage, frequency, duration, instructions)
}

fun MedicalRecord.toEntity() = MedicalRecordEntity(
    id = id,
    appointmentId = appointmentId,
    consultationId = consultationId,
    doctorId = doctorId,
    patientId = patientId,
    diagnosis = diagnosis,
    prescription = prescription.map { PrescriptionItemEntity(it.drug, it.dosage, it.frequency, it.duration, it.instructions) },
    summary = summary,
    referralNote = referralNote,
    labRequests = labRequests,
    additionalNotes = additionalNotes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
