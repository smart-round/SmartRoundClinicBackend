package ke.co.smartroundclinic.consultation.domain.service

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.presentation.dto.request.ConsultationWsMessage
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import java.util.Base64
import kotlin.time.Duration.Companion.days

class ConsultationChatService(
    private val repository: ConsultationMessageRepository,
    private val storageRepository: StorageRepository,
    private val historyUseCase: GetConsultationHistoryUseCase,
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

    suspend fun handleIncomingMessage(
        consultationId: String,
        senderId: String,
        senderRole: String,
        senderName: String,
        rawJson: String,
    ) {
        val msg = json.decodeFromString<ConsultationWsMessage>(rawJson)
        when (msg.type) {
            MessageType.TEXT -> {
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
            }
            MessageType.FILE -> {
                val rawData = msg.data ?: return
                val fileName = msg.fileName?.takeIf { it.isNotBlank() } ?: "file"
                val contentType = msg.contentType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
                val bytes = Base64.getDecoder().decode(rawData)
                val messageId = ObjectId().toString()
                val ext = fileName.substringAfterLast(".", "bin")
                val key = "consultation-files/$consultationId/$messageId.$ext"

                val uploadResult = storageRepository.upload(AppConfig.r2.bucket, key, bytes, contentType)
                if (uploadResult is Resource.Error) return

                val fileUrl = when (val urlResult = storageRepository.presignedGetUrl(AppConfig.r2.bucket, key, 6.days.inWholeSeconds)) {
                    is Resource.Success -> urlResult.data ?: key
                    is Resource.Error -> key
                }

                repository.save(
                    ConsultationMessageEntity(
                        id = messageId,
                        consultationId = consultationId,
                        senderId = senderId,
                        senderRole = senderRole,
                        senderName = senderName,
                        messageType = MessageType.FILE,
                        files = listOf(
                            ConsultationFile(
                                fileName = fileName,
                                url = fileUrl,
                                contentType = contentType,
                                sizeBytes = bytes.size.toLong(),
                            )
                        ),
                    )
                )
            }
        }
    }
}
