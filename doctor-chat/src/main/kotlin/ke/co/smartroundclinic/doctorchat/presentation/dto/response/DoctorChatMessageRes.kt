package ke.co.smartroundclinic.doctorchat.presentation.dto.response

import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatFile
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatMessageType
import ke.co.smartroundclinic.doctorchat.domain.model.DoctorChatMessage
import kotlinx.serialization.Serializable

@Serializable
data class DoctorChatMessageRes(
    val id: String,
    val threadId: String,
    val senderId: String,
    val senderName: String,
    val messageType: DoctorChatMessageType,
    val message: String?,
    val files: List<DoctorChatFile>,
    val createdAt: String,
)

@Serializable
data class DoctorChatMessagesPageRes(
    val items: List<DoctorChatMessageRes>,
    val nextCursor: String?,
)

fun DoctorChatMessage.toRes() = DoctorChatMessageRes(
    id = id,
    threadId = threadId,
    senderId = senderId,
    senderName = senderName,
    messageType = messageType,
    message = message,
    files = files,
    createdAt = createdAt,
)
