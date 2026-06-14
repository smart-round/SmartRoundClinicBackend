package ke.co.smartroundclinic.consultation.domain.service

import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.NotifyOfflineConsultationParticipantUseCase
import ke.co.smartroundclinic.consultation.presentation.dto.request.ConsultationWsMessage
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import kotlin.time.Duration.Companion.days

class ConsultationChatService(
    private val repository: ConsultationMessageRepository,
    private val storageRepository: StorageRepository,
    private val historyUseCase: GetConsultationHistoryUseCase,
    private val notifyOfflineParticipant: NotifyOfflineConsultationParticipantUseCase,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUserName(userId: String): String? = repository.getUserName(userId)

    suspend fun getHistory(consultationId: String, page: Int, size: Int) =
        historyUseCase(consultationId, page, size)

    fun watchMessages(consultationId: String): Flow<ConsultationMessageEntity> =
        repository.watchMessages(consultationId)

    suspend fun getRecentHistory(consultationId: String): List<ConsultationMessageEntity> =
        when (val result = repository.getByConsultationId(consultationId, 1, 50)) {
            is Resource.Success -> result.data?.first ?: emptyList()
            is Resource.Error -> emptyList()
        }

    /**
     * WebSocket-only handler for incoming text messages from a connected client.
     * File uploads no longer travel over the WebSocket — clients call
     * `POST /consultation/{id}/files` (multipart) instead. The MongoDB change
     * stream still broadcasts the resulting FILE message to all connected sockets.
     */
    suspend fun handleIncomingMessage(
        consultationId: String,
        appointmentId: String,
        senderId: String,
        senderRole: String,
        senderName: String,
        rawJson: String,
        recipientId: String,
        recipientDestination: NotificationDestination,
    ) {
        val msg = json.decodeFromString<ConsultationWsMessage>(rawJson)
        if (msg.type != MessageType.TEXT) return
        val text = msg.message?.takeIf { it.isNotBlank() } ?: return
        repository.save(
            ConsultationMessageEntity(
                consultationId = consultationId,
                senderId = senderId,
                senderRole = senderRole,
                senderName = senderName,
                messageType = MessageType.TEXT,
                message = text,
            )
        )
        runCatching {
            notifyOfflineParticipant(
                recipientId = recipientId,
                senderName = senderName,
                messagePreview = text,
                recipientDestination = recipientDestination,
                consultationId = consultationId,
                appointmentId = appointmentId,
            )
        }
    }

    /**
     * Uploads a file to R2 under `consultation-files/{consultationId}/{messageId}.{ext}`
     * and persists a FILE-type message. The change stream pushes the new message
     * to any connected WebSocket clients.
     */
    suspend fun uploadFile(
        consultationId: String,
        senderId: String,
        senderRole: String,
        senderName: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
    ): Resource<ConsultationMessageEntity> {
        if (bytes.isEmpty()) return Resource.Error("Empty file")
        val safeName = fileName.ifBlank { "file" }
        val safeContentType = contentType.ifBlank { "application/octet-stream" }
        val messageId = ObjectId().toString()
        val ext = safeName.substringAfterLast(".", "bin")
        val key = "consultation-files/$consultationId/$messageId.$ext"

        val uploadResult = storageRepository.upload(AppConfig.r2.bucket, key, bytes, safeContentType)
        if (uploadResult is Resource.Error) {
            return Resource.Error(uploadResult.message ?: "Failed to upload file")
        }

        val fileUrl = when (val urlResult = storageRepository.presignedGetUrl(AppConfig.r2.bucket, key, 6.days.inWholeSeconds)) {
            is Resource.Success -> urlResult.data ?: key
            is Resource.Error -> key
        }

        val entity = ConsultationMessageEntity(
            id = messageId,
            consultationId = consultationId,
            senderId = senderId,
            senderRole = senderRole,
            senderName = senderName,
            messageType = MessageType.FILE,
            files = listOf(
                ConsultationFile(
                    fileName = safeName,
                    url = fileUrl,
                    contentType = safeContentType,
                    sizeBytes = bytes.size.toLong(),
                )
            ),
        )
        return when (val saveResult = repository.save(entity)) {
            is Resource.Success -> Resource.Success(saveResult.data ?: entity)
            is Resource.Error -> Resource.Error(saveResult.message ?: "Failed to save file message")
        }
    }
}
