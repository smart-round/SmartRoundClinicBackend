package ke.co.smartroundclinic.support.domain.service

import ke.co.smartroundclinic.support.data.entity.ChatFile
import ke.co.smartroundclinic.support.data.entity.MessageType
import ke.co.smartroundclinic.support.data.entity.SupportTicketChatEntity
import ke.co.smartroundclinic.support.domain.repository.SupportTicketChatRepository
import ke.co.smartroundclinic.support.presentation.dto.request.WsChatMessage
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import java.util.Base64
import kotlin.time.Duration.Companion.days

class SupportTicketChatService(
    private val repository: SupportTicketChatRepository,
    private val storageRepository: StorageRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUserName(userId: String): String? = repository.getUserName(userId)

    suspend fun getHistory(ticketId: String): List<SupportTicketChatEntity> =
        when (val result = repository.getByTicketId(ticketId)) {
            is Resource.Success -> result.data ?: emptyList()
            is Resource.Error -> emptyList()
        }

    fun watchMessages(ticketId: String): Flow<SupportTicketChatEntity> =
        repository.watchMessages(ticketId)

    suspend fun handleIncomingMessage(
        ticketId: String,
        senderId: String,
        senderName: String,
        rawJson: String,
    ) {
        val msg = json.decodeFromString<WsChatMessage>(rawJson)
        when (msg.type) {
            MessageType.TEXT -> {
                val text = msg.message?.takeIf { it.isNotBlank() } ?: return
                repository.save(
                    SupportTicketChatEntity(
                        ticketId = ticketId,
                        senderId = senderId,
                        senderName = senderName,
                        messageType = MessageType.TEXT,
                        message = text,
                    )
                )
            }
            MessageType.FILE -> {
                val rawData = msg.data ?: return
                val fileName = msg.fileName?.takeIf { it.isNotBlank() } ?: "file"
                val contentType = msg.contentType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
                val bytes = Base64.getDecoder().decode(rawData)
                val entityId = ObjectId().toString()
                val ext = fileName.substringAfterLast(".", "bin")
                val key = "support-chat-files/$ticketId/$entityId.$ext"

                val uploadResult = storageRepository.upload(AppConfig.r2.bucket, key, bytes, contentType)
                if (uploadResult is Resource.Error) return

                val fileUrl = when (val urlResult = storageRepository.presignedGetUrl(AppConfig.r2.bucket, key, 120.days.inWholeSeconds)) {
                    is Resource.Success -> urlResult.data ?: key
                    is Resource.Error -> key
                }

                repository.save(
                    SupportTicketChatEntity(
                        id = entityId,
                        ticketId = ticketId,
                        senderId = senderId,
                        senderName = senderName,
                        messageType = MessageType.FILE,
                        files = listOf(ChatFile(fileName = fileName, url = fileUrl, contentType = contentType)),
                    )
                )
            }
        }
    }
}
