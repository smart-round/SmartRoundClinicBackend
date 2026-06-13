package ke.co.smartroundclinic.support.domain.service

import ke.co.smartroundclinic.support.data.entity.ChatFile
import ke.co.smartroundclinic.support.data.entity.MessageType
import ke.co.smartroundclinic.support.data.entity.SupportTicketChatEntity
import ke.co.smartroundclinic.support.domain.repository.SupportTicketChatRepository
import ke.co.smartroundclinic.support.domain.usecase.chat.NotifyOfflineSupportParticipantUseCase
import ke.co.smartroundclinic.support.presentation.dto.request.WsChatMessage
import ke.co.smartroundclinic.support.presentation.dto.response.SupportTicketChatRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import kotlin.time.Duration.Companion.days

class SupportTicketChatService(
    private val repository: SupportTicketChatRepository,
    private val storageRepository: StorageRepository,
    private val notifyOfflineParticipant: NotifyOfflineSupportParticipantUseCase,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUserName(userId: String): String? = repository.getUserName(userId)

    suspend fun getComplainantId(ticketId: String): String? = repository.getComplainantId(ticketId)

    suspend fun getHistory(ticketId: String): List<SupportTicketChatEntity> =
        when (val result = repository.getByTicketId(ticketId)) {
            is Resource.Success -> result.data ?: emptyList()
            is Resource.Error -> emptyList()
        }

    suspend fun getChatHistory(ticketId: String): DefaultResponse<List<SupportTicketChatRes?>?> =
        repository.getByTicketId(ticketId)
            .toDefaultResponse(failedStatusCode = 404) { entities ->
                entities?.map { it.toModel().toRes() }
            }

    fun watchMessages(ticketId: String): Flow<SupportTicketChatEntity> =
        repository.watchMessages(ticketId)

    suspend fun handleIncomingMessage(
        ticketId: String,
        senderId: String,
        senderName: String,
        rawJson: String,
        recipientId: String? = null,
    ) {
        val msg = json.decodeFromString<WsChatMessage>(rawJson)
        val text = msg.message.takeIf { it.isNotBlank() } ?: return
        repository.save(
            SupportTicketChatEntity(
                ticketId = ticketId,
                senderId = senderId,
                senderName = senderName,
                messageType = MessageType.TEXT,
                message = text,
            )
        )
        if (recipientId != null) {
            runCatching {
                notifyOfflineParticipant(
                    recipientId = recipientId,
                    senderName = senderName,
                    messagePreview = text,
                )
            }
        }
    }

    suspend fun uploadFile(
        ticketId: String,
        senderId: String,
        senderName: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
    ): DefaultResponse<SupportTicketChatRes?> {
        val entityId = ObjectId().toString()
        val ext = fileName.substringAfterLast(".", "bin")
        val key = "support-chat-files/$ticketId/$entityId.$ext"

        val uploadResult = storageRepository.upload(AppConfig.r2.bucket, key, bytes, contentType)
        if (uploadResult is Resource.Error) {
            return DefaultResponse(500, false, "Failed to upload file")
        }

        val fileUrl = when (val urlResult = storageRepository.presignedGetUrl(AppConfig.r2.bucket, key, 6.days.inWholeSeconds)) {
            is Resource.Success -> urlResult.data ?: key
            is Resource.Error -> key
        }

        val entity = SupportTicketChatEntity(
            id = entityId,
            ticketId = ticketId,
            senderId = senderId,
            senderName = senderName,
            messageType = MessageType.FILE,
            files = listOf(ChatFile(fileName = fileName, url = fileUrl, contentType = contentType)),
        )
        return when (repository.save(entity)) {
            is Resource.Success -> DefaultResponse(200, true, "File uploaded", entity.toModel().toRes())
            is Resource.Error -> DefaultResponse(500, false, "Failed to save file message")
        }
    }
}
