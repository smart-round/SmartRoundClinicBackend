package ke.co.smartroundclinic.support.presentation.dto.response

import ke.co.smartroundclinic.support.data.entity.ChatFile
import ke.co.smartroundclinic.support.data.entity.MessageType
import ke.co.smartroundclinic.support.domain.model.SupportTicketChat
import kotlinx.serialization.Serializable

@Serializable
data class SupportTicketChatRes(
    val id: String,
    val ticketId: String,
    val senderId: String,
    val senderName: String,
    val messageType: MessageType,
    val message: String?,
    val files: List<ChatFile>,
    val createdAt: String,
    val updatedAt: String?,
)

fun SupportTicketChat.toRes() = SupportTicketChatRes(
    id = id,
    ticketId = ticketId,
    senderId = senderId,
    senderName = senderName,
    messageType = messageType,
    message = message,
    files = files,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
