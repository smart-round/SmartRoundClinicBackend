package ke.co.smartroundclinic.support.domain.model

import ke.co.smartroundclinic.support.data.entity.ChatFile
import ke.co.smartroundclinic.support.data.entity.MessageType

data class SupportTicketChat(
    val id: String,
    val ticketId: String,
    val senderId: String,
    val senderName: String,
    val messageType: MessageType,
    val message: String?,
    val files: List<ChatFile>,
    val createdAt: String,
    val updatedAt: String? = null,
)
