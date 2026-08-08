package ke.co.smartroundclinic.consultation.domain.service

import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.usecase.chat.NotifyOfflineConsultationParticipantUseCase
import ke.co.smartroundclinic.consultation.presentation.dto.request.ConsultationWsMessage
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationTypingEventRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.redis.RedisKeys
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

private const val FILE_URL_TTL = 86400L  // 24 hours

class ConsultationChatService(
    private val repository: ConsultationMessageRepository,
    private val storageRepository: StorageRepository,
    private val notifyOfflineParticipant: NotifyOfflineConsultationParticipantUseCase,
    private val socketRegistry: ConsultationSocketRegistry,
    private val redis: RedisRepository,
) {
    private val log = LoggerFactory.getLogger(ConsultationChatService::class.java)
    // encodeDefaults = true — the "type" discriminator on ConsultationTypingEventRes defaults to
    // "TYPING", so without this it's always equal to its default and gets omitted entirely from the
    // wire payload, leaving clients with no "type" field to dispatch on.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun getUserName(userId: String): String? = repository.getUserName(userId)

    suspend fun isOnline(userId: String): Boolean = redis.get(RedisKeys.presence(userId)) == "true"

    suspend fun getLastSeenAt(userId: String): String? = repository.getLastSeenAt(userId)

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
     * WebSocket-only handler for incoming text/typing frames from a connected client.
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
                val recipientConnected = socketRegistry.hasOpenSession(threadKey, recipientId)
                log.info(
                    "TYPING relay: sender=$senderId recipient=$recipientId threadKey=$threadKey " +
                        "isTyping=$isTyping recipientHasOpenSession=$recipientConnected",
                )
                socketRegistry.sendToUser(
                    threadKey,
                    recipientId,
                    json.encodeToString(ConsultationTypingEventRes(senderId = senderId, isTyping = isTyping)),
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
    /**
     * Step 1 of the direct-to-R2 upload: hands the client a pre-signed PUT URL.
     *
     * The legacy [uploadFile] path streams the whole file through this process and holds it in
     * heap before forwarding it to R2, so the transfer is paid for twice and large files
     * threaten the JVM. Here the bytes never touch us — the client PUTs straight to storage and
     * then calls [completeFileUpload] to record the message.
     *
     * The messageId is minted now and reused as the object key, so a client that uploads but
     * never completes leaves an orphan object rather than a phantom message.
     */
    suspend fun presignFileUpload(
        doctorId: String,
        patientId: String,
        fileName: String,
        contentType: String,
    ): Resource<PresignedUpload> {
        val safeName = fileName.ifBlank { "file" }
        val safeContentType = contentType.ifBlank { "application/octet-stream" }
        val messageId = ObjectId().toString()
        val ext = safeName.substringAfterLast(".", "bin")
        val key = "consultation-files/$doctorId-$patientId/$messageId.$ext"

        return when (val result = storageRepository.presignedPutUrl(AppConfig.r2.bucket, key, safeContentType)) {
            is Resource.Success -> Resource.Success(
                PresignedUpload(
                    messageId = messageId,
                    key = key,
                    uploadUrl = result.data ?: return Resource.Error("No upload URL returned"),
                    contentType = safeContentType,
                ),
            )
            is Resource.Error -> Resource.Error(result.message ?: "Failed to prepare upload")
        }
    }

    /**
     * Step 2: records the message for an object the client has already PUT to R2.
     *
     * [key] is not taken on trust — it must match the key we minted for [messageId] under this
     * exact thread, so a caller cannot point a message at somebody else's object.
     */
    suspend fun completeFileUpload(
        doctorId: String,
        patientId: String,
        senderId: String,
        senderRole: String,
        senderName: String,
        messageId: String,
        key: String,
        fileName: String,
        contentType: String,
        sizeBytes: Long,
    ): Resource<ConsultationMessageEntity> {
        val safeName = fileName.ifBlank { "file" }
        val safeContentType = contentType.ifBlank { "application/octet-stream" }
        val expectedPrefix = "consultation-files/$doctorId-$patientId/$messageId."
        if (!key.startsWith(expectedPrefix)) {
            return Resource.Error("Upload key does not belong to this conversation")
        }
        if (sizeBytes <= 0) return Resource.Error("Empty file")

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
                    url = key, // store R2 key, not presigned URL
                    contentType = safeContentType,
                    sizeBytes = sizeBytes,
                ),
            ),
        )
        return when (val saveResult = repository.save(entity)) {
            is Resource.Success -> Resource.Success(resolveEntityFiles(saveResult.data ?: entity))
            is Resource.Error -> Resource.Error(saveResult.message ?: "Failed to save file message")
        }
    }

    /** Legacy proxied upload — kept so older clients keep working. Prefer the pre-signed flow. */
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

/** What the client needs to PUT a chat attachment straight to R2. */
data class PresignedUpload(
    val messageId: String,
    val key: String,
    val uploadUrl: String,
    val contentType: String,
)
