package ke.co.smartroundclinic.medicalrecords.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.medicalrecords.data.entity.toEntity
import ke.co.smartroundclinic.medicalrecords.domain.model.MedicalRecord
import ke.co.smartroundclinic.medicalrecords.domain.repository.MedicalRecordRepository
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.MedicalRecordRes
import ke.co.smartroundclinic.medicalrecords.presentation.dto.response.toRes
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SaveMedicalRecordUseCase(
    private val repository: MedicalRecordRepository,
    private val messageRepository: ConsultationMessageRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend operator fun invoke(model: MedicalRecord, senderName: String): DefaultResponse<MedicalRecordRes?> {
        val result = repository.upsert(model.toEntity())
        if (result is Resource.Success && model.consultationId != null && model.prescription.isNotEmpty()) {
            runCatching {
                messageRepository.save(
                    ConsultationMessageEntity(
                        consultationId = model.consultationId,
                        senderId = model.doctorId,
                        senderRole = "DOCTOR",
                        senderName = senderName,
                        messageType = MessageType.PRESCRIPTION,
                        message = json.encodeToString(model.toRes()),
                    )
                )
            }
        }
        return result.toDefaultResponse(
            successStatusCode = 200,
            failedStatusCode = 400,
        ) { it?.toModel()?.toRes() }
    }
}
