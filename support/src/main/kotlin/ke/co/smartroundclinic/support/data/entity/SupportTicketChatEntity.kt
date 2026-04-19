package ke.co.smartroundclinic.support.data.entity

import ke.co.smartroundclinic.support.domain.model.SupportTicketChat
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

data class SupportTicketChatEntity(
    val id: String = ObjectId().toString(),
    val ticketId: String,
    val senderId: String,
    val senderName: String,
    val messageType: MessageType = MessageType.TEXT,
    val message: String? = null,
    val files: List<ChatFile> = emptyList(),
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = SupportTicketChat(
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
}

@Serializable
enum class MessageType { TEXT, FILE }

@Serializable
data class ChatFile(
    val fileName: String,
    val url: String,
    val contentType: String,
)

fun SupportTicketChat.toEntity() = SupportTicketChatEntity(
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
