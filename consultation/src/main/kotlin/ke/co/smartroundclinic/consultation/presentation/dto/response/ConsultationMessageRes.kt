package ke.co.smartroundclinic.consultation.presentation.dto.response

import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.model.ConsultationMessage
import kotlinx.serialization.Serializable

@Serializable
data class ConsultationMessageRes(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val senderId: String,
    val senderRole: String,
    val senderName: String,
    val messageType: MessageType,
    val message: String?,
    val files: List<ConsultationFile>,
    val appointmentId: String? = null,
    val createdAt: String,
    val updatedAt: String?,
)

/** Merged, cursor-paginated page for [ke.co.smartroundclinic.consultation.domain.usecase.chat.GetMergedConsultationHistoryUseCase]. */
@Serializable
data class ConversationThreadMessagesRes(
    val items: List<ConsultationMessageRes>,
    val nextCursor: String?,
    /** The counterpart's read/delivered watermarks, for computing tick state on the requester's own sent messages. */
    val counterpartLastReadAt: String? = null,
    val counterpartLastDeliveredAt: String? = null,
)

fun ConsultationMessage.toRes() = ConsultationMessageRes(
    id = id,
    doctorId = doctorId,
    patientId = patientId,
    senderId = senderId,
    senderRole = senderRole,
    senderName = senderName,
    messageType = messageType,
    message = message,
    files = files,
    appointmentId = appointmentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
