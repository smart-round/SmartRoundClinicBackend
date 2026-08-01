package ke.co.smartroundclinic.doctorchat.domain.service

import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatFile
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatMessageEntity
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatMessageType
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
import ke.co.smartroundclinic.doctorchat.presentation.dto.request.DoctorChatWsMessage
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorTypingEventRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.redis.RedisKeys
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

private const val FILE_URL_TTL = 86400L  // 24 hours

/**
 * Mirrors [ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService], including
 * presence/typing relay — presence reuses the same global `/auth/user/presence` heartbeat (Redis
 * key is user-scoped, not doctor/patient-scoped, so no new infra is needed here).
 */
class DoctorChatService(
    private val repository: DoctorChatMessageRepository,
    private val storageRepository: StorageRepository,
    private val socketRegistry: DoctorChatSocketRegistry,
    private val redis: RedisRepository,
    private val notificationSender: NotificationSender? = null,
) {
    private val log = LoggerFactory.getLogger(DoctorChatService::class.java)
    // encodeDefaults = true — the "type" discriminator on DoctorTypingEventRes defaults to "TYPING",
    // so without this it's always equal to its default and gets omitted from the wire payload,
    // leaving clients with no "type" field to dispatch on.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun getUserName(userId: String): String? = repository.getUserName(userId)

    suspend fun isOnline(userId: String): Boolean = redis.get(RedisKeys.presence(userId)) == "true"

    suspend fun getLastSeenAt(userId: String): String? = repository.getLastSeenAt(userId)

    fun watchMessagesForThread(threadId: String): Flow<DoctorChatMessageEntity> =
        repository.watchMessagesForThread(threadId)

    suspend fun resolveEntityFiles(entity: DoctorChatMessageEntity): DoctorChatMessageEntity {
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

    /** WebSocket-only handler for incoming TEXT/TYPING frames. File uploads go through the REST endpoint instead. */
    suspend fun handleIncomingMessage(
        threadId: String,
        senderId: String,
        senderName: String,
        rawJson: String,
        recipientId: String,
    ) {
        val msg = json.decodeFromString<DoctorChatWsMessage>(rawJson)
        when (msg.type) {
            DoctorChatMessageType.TEXT.name -> {
                val text = msg.message?.takeIf { it.isNotBlank() } ?: return
                repository.save(
                    DoctorChatMessageEntity(
                        threadId = threadId,
                        senderId = senderId,
                        senderName = senderName,
                        messageType = DoctorChatMessageType.TEXT,
                        message = text,
                    )
                )
                if (!isOnline(recipientId)) {
                    runCatching {
                        notificationSender?.send(
                            title = PushNotificationEvents.newChatMessage(senderName),
                            message = text.take(100),
                            channel = NotificationChannel.PUSH_NOTIFICATION,
                            destination = NotificationDestination.DOCTOR,
                            recipientId = recipientId,
                            metadata = mapOf(
                                "event" to PushNotificationEvents.NEW_CHAT_MESSAGE,
                                "threadId" to threadId,
                            ),
                        )
                    }
                }
            }
            "TYPING" -> {
                val isTyping = msg.isTyping ?: return
                val recipientConnected = socketRegistry.hasOpenSession(threadId, recipientId)
                log.debug("TYPING relay: sender=$senderId recipient=$recipientId threadId=$threadId isTyping=$isTyping recipientHasOpenSession=$recipientConnected")
                socketRegistry.sendToUser(
                    threadId, recipientId,
                    json.encodeToString(DoctorTypingEventRes(senderId = senderId, isTyping = isTyping)),
                )
            }
            else -> return
        }
    }

    /** Uploads to R2 under `doctor-chat-files/{threadId}/{messageId}.{ext}` and persists a FILE message. */
    suspend fun uploadFile(
        threadId: String,
        senderId: String,
        senderName: String,
        recipientId: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
    ): Resource<DoctorChatMessageEntity> {
        if (bytes.isEmpty()) return Resource.Error("Empty file")
        val safeName = fileName.ifBlank { "file" }
        val safeContentType = contentType.ifBlank { "application/octet-stream" }
        val messageId = ObjectId().toString()
        val ext = safeName.substringAfterLast(".", "bin")
        val key = "doctor-chat-files/$threadId/$messageId.$ext"

        val uploadResult = storageRepository.upload(AppConfig.r2.bucket, key, bytes, safeContentType)
        if (uploadResult is Resource.Error) {
            return Resource.Error(uploadResult.message ?: "Failed to upload file")
        }

        val entity = DoctorChatMessageEntity(
            id = messageId,
            threadId = threadId,
            senderId = senderId,
            senderName = senderName,
            messageType = DoctorChatMessageType.FILE,
            files = listOf(DoctorChatFile(fileName = safeName, url = key, contentType = safeContentType, sizeBytes = bytes.size.toLong())),
        )
        val result = when (val saveResult = repository.save(entity)) {
            is Resource.Success -> Resource.Success(resolveEntityFiles(saveResult.data ?: entity))
            is Resource.Error -> Resource.Error(saveResult.message ?: "Failed to save file message")
        }
        if (!isOnline(recipientId)) {
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.newChatMessage(senderName),
                    message = safeName,
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.DOCTOR,
                    recipientId = recipientId,
                    metadata = mapOf("event" to PushNotificationEvents.NEW_CHAT_MESSAGE, "threadId" to threadId),
                )
            }
        }
        return result
    }
}
