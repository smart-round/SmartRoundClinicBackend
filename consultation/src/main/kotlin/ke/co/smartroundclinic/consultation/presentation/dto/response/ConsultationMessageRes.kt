package ke.co.smartroundclinic.consultation.presentation.dto.response

import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.model.ConsultationMessage
import kotlinx.serialization.Serializable

@Serializable
data class ConsultationMessageRes(
    val id: String,
    val consultationId: String,
    val doctorId: String,
    val patientId: String,
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

/** Merged, cursor-paginated page for [ke.co.smartroundclinic.consultation.domain.usecase.chat.GetMergedConsultationHistoryUseCase]. */
@Serializable
data class ConversationThreadMessagesRes(
    val items: List<ConsultationMessageRes>,
    val nextCursor: String?,
)

fun ConsultationMessage.toRes() = ConsultationMessageRes(
    id = id,
    consultationId = consultationId,
    doctorId = doctorId,
    patientId = patientId,
    senderId = senderId,
    senderRole = senderRole,
    senderName = senderName,
    messageType = messageType,
    message = message,
    files = files,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
