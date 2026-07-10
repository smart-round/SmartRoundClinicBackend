package ke.co.smartroundclinic.consultation.domain.service

import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadReadStateRepository
import ke.co.smartroundclinic.consultation.domain.usecase.chat.NotifyOfflineConsultationParticipantUseCase
import ke.co.smartroundclinic.consultation.presentation.dto.request.ConsultationWsMessage
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationReadReceiptEventRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationTypingEventRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.redis.RedisKeys
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId

private const val FILE_URL_TTL = 86400L  // 24 hours

class ConsultationChatService(
    private val repository: ConsultationMessageRepository,
    private val storageRepository: StorageRepository,
    private val notifyOfflineParticipant: NotifyOfflineConsultationParticipantUseCase,
    private val socketRegistry: ConsultationSocketRegistry,
    private val redis: RedisRepository,
    private val readStateRepository: ConsultationThreadReadStateRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUserName(userId: String): String? = repository.getUserName(userId)

    suspend fun isOnline(userId: String): Boolean = redis.get(RedisKeys.presence(userId)) == "true"

    suspend fun getLastSeenAt(userId: String): String? = repository.getLastSeenAt(userId)

    suspend fun bumpDelivered(doctorId: String, patientId: String, recipientId: String, at: String) {
        readStateRepository.bumpDelivered(doctorId, patientId, recipientId, at)
    }

    fun watchMessagesForThread(doctorId: String, patientId: String): Flow<ConsultationMessageEntity> =
        repository.watchMessagesForThread(doctorId, patientId)

    suspend fun resolveEntityFiles(entity: ConsultationMessageEntity): ConsultationMessageEntity {
        if (entity.files.isEmpty()) return entity
        val resolved = entity.files.map { file ->
            if (file.url.startsWith("https://")) file
            else {
                val url = (storageRepository.presignedGetUrl(AppConfig.r2.bucket, file.url, FILE_URL_TTL) as? Resource.Success)?.data
                if (url != null) file.copy(url = url) else file
            }
        }
        return entity.copy(files = resolved)
    }

    /**
     * WebSocket-only handler for incoming text/typing/read frames from a connected client.
     * File uploads no longer travel over the WebSocket — clients call
     * `POST /chat/{otherUserId}/files` (multipart) instead. The MongoDB change
     * stream still broadcasts the resulting FILE message to all connected sockets.
     */
    suspend fun handleIncomingMessage(
        doctorId: String,
        patientId: String,
        senderId: String,
        senderRole: String,
        senderName: String,
        rawJson: String,
        recipientId: String,
        recipientDestination: NotificationDestination,
    ) {
        val msg = json.decodeFromString<ConsultationWsMessage>(rawJson)
        when (msg.type) {
            MessageType.TEXT.name -> {
                val text = msg.message?.takeIf { it.isNotBlank() } ?: return
                repository.save(
                    ConsultationMessageEntity(
                        doctorId = doctorId,
                        patientId = patientId,
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
                        doctorId = doctorId,
                        patientId = patientId,
                    )
                }
            }
            "TYPING" -> {
                val isTyping = msg.isTyping ?: return
                val threadKey = ConsultationSocketRegistry.threadKey(doctorId, patientId)
                socketRegistry.sendToUser(
                    threadKey,
                    recipientId,
                    json.encodeToString(ConsultationTypingEventRes(senderId = senderId, isTyping = isTyping)),
                )
            }
            // Sent by a client that already has this conversation open when a new message arrives —
            // keeps the sender's ticks flipping to "read" live, instead of only once per REST fetch
            // (see markThreadReadAndRelay in ConversationThreadController for the REST-triggered path).
            "READ" -> {
                val readState = readStateRepository.markRead(doctorId, patientId, senderId, sortableNowIso())
                val lastReadAt = (readState as? Resource.Success)?.data?.let {
                    if (senderId == doctorId) it.doctorLastReadAt else it.patientLastReadAt
                } ?: return
                val threadKey = ConsultationSocketRegistry.threadKey(doctorId, patientId)
                socketRegistry.sendToUser(
                    threadKey,
                    recipientId,
                    json.encodeToString(
                        ConsultationReadReceiptEventRes(doctorId = doctorId, patientId = patientId, readerId = senderId, lastReadAt = lastReadAt)
                    ),
                )
            }
            else -> return
        }
    }

    /**
     * Uploads a file to R2 under `consultation-files/{doctorId}-{patientId}/{messageId}.{ext}`
     * and persists a FILE-type message. The change stream pushes the new message
     * to any connected WebSocket clients.
     */
    suspend fun uploadFile(
        doctorId: String,
        patientId: String,
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
        val key = "consultation-files/$doctorId-$patientId/$messageId.$ext"

        val uploadResult = storageRepository.upload(AppConfig.r2.bucket, key, bytes, safeContentType)
        if (uploadResult is Resource.Error) {
            return Resource.Error(uploadResult.message ?: "Failed to upload file")
        }

        val entity = ConsultationMessageEntity(
            id = messageId,
            doctorId = doctorId,
            patientId = patientId,
            senderId = senderId,
            senderRole = senderRole,
            senderName = senderName,
            messageType = MessageType.FILE,
            files = listOf(
                ConsultationFile(
                    fileName = safeName,
                    url = key,  // store R2 key, not presigned URL
                    contentType = safeContentType,
                    sizeBytes = bytes.size.toLong(),
                )
            ),
        )
        return when (val saveResult = repository.save(entity)) {
            is Resource.Success -> Resource.Success(resolveEntityFiles(saveResult.data ?: entity))
            is Resource.Error -> Resource.Error(saveResult.message ?: "Failed to save file message")
        }
    }
}
