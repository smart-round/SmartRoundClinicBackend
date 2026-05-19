package ke.co.smartroundclinic.consultation.domain.model

import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.MessageType

data class ConsultationMessage(
    val id: String,
    val consultationId: String,
    val senderId: String,
    val senderRole: String,
    val senderName: String,
    val messageType: MessageType,
    val message: String?,
    val files: List<ConsultationFile>,
    val createdAt: String,
    val updatedAt: String?,
)
