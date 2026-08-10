package ke.co.smartroundclinic.medicalrecords.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.usecase.chat.NotifyOfflineConsultationParticipantUseCase
import ke.co.smartroundclinic.medicalrecords.data.entity.toEntity
import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecord
import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecordSaveResult
import ke.co.smartroundclinic.medicalrecords.domain.repository.MedicalRecordRepository
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.MedicalRecordRes
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.toRes
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Saves (or revises) a visit's medical record and, when there is something clinical for the patient
 * to see, drops a card into their chat thread with the doctor.
 *
 * The thread is append-only: a revision posts a *new* card carrying the whole current record and
 * tagged with the fields that changed, rather than mutating the card already in the history.
 */
class SaveMedicalRecordUseCase(
    private val repository: MedicalRecordRepository,
    private val messageRepository: ConsultationMessageRepository,
    private val notifyOfflineParticipant: NotifyOfflineConsultationParticipantUseCase? = null,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend operator fun invoke(model: MedicalRecord, senderName: String): DefaultResponse<MedicalRecordRes?> {
        val result = repository.upsert(model.toEntity())

        if (result is Resource.Success) {
            result.data?.let { saved ->
                if (shouldNotify(saved)) {
                    runCatching { postCardToChat(saved, senderName) }
                }
            }
        }

        return result.toDefaultResponse(
            successStatusCode = 200,
            failedStatusCode = 400,
        ) { it?.record?.toModel()?.toRes() }
    }

    /**
     * A first save always reaches the patient. A revision only does when a notifiable field
     * actually changed — a doctor rewording their own summary or notes should not ping anyone.
     * Their latest prose still rides along on the next card that is posted, because every card
     * carries the whole record.
     */
    private fun shouldNotify(saved: MedicalRecordSaveResult): Boolean =
        !saved.isUpdate || saved.changedFields.isNotEmpty()

    private suspend fun postCardToChat(saved: MedicalRecordSaveResult, senderName: String) {
        val record = saved.record
        // The persisted entity, not the incoming request model: only this carries the real id,
        // createdAt and — on a revision — updatedAt, which is what marks the card as an edit.
        val payload = record.toModel().toRes().copy(
            editedFields = saved.changedFields.takeIf { saved.isUpdate }?.map { it.name } ?: emptyList(),
        )

        messageRepository.save(
            ConsultationMessageEntity(
                doctorId = record.doctorId,
                patientId = record.patientId,
                appointmentId = record.appointmentId,
                senderId = record.doctorId,
                senderRole = "DOCTOR",
                senderName = senderName,
                messageType = MessageType.PRESCRIPTION,
                message = json.encodeToString(payload),
            )
        )

        // The change stream pushes the insert to a patient who has the thread open; this covers the
        // one who does not, so the card behaves like any other message rather than going unseen.
        runCatching {
            notifyOfflineParticipant?.invoke(
                recipientId = record.patientId,
                senderName = senderName,
                messagePreview = if (record.prescription.isEmpty()) "Medical record updated" else "Prescription",
                recipientDestination = NotificationDestination.PATIENT,
                doctorId = record.doctorId,
                patientId = record.patientId,
            )
        }
    }
}
