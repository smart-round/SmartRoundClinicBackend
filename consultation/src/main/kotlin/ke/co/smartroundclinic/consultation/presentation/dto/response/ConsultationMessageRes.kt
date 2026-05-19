package ke.co.smartroundclinic.consultation.presentation.dto.response

import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.model.ConsultationMessage
import kotlinx.serialization.Serializable

@Serializable
data class ConsultationMessageRes(
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

@Serializable
data class ConsultationMessagePageRes(
    val items: List<ConsultationMessageRes>,
    val total: Long,
    val page: Int,
    val size: Int,
)

fun ConsultationMessage.toRes() = ConsultationMessageRes(
    id = id,
    consultationId = consultationId,
    senderId = senderId,
    senderRole = senderRole,
    senderName = senderName,
    messageType = messageType,
    message = message,
    files = files,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
