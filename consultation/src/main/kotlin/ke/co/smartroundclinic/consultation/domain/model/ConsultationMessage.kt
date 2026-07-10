package ke.co.smartroundclinic.consultation.domain.model

import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.MessageType

data class ConsultationMessage(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val senderId: String,
    val senderRole: String,
    val senderName: String,
    val messageType: MessageType,
    val message: String?,
    val files: List<ConsultationFile>,
    val appointmentId: String?,
    val createdAt: String,
    val updatedAt: String?,
)
