package ke.co.smartroundclinic.consultation.data.entity

import ke.co.smartroundclinic.consultation.domain.model.ConsultationMessage
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

data class ConsultationMessageEntity(
    val id: String = ObjectId().toString(),
    val consultationId: String,
    val doctorId: String = "",
    val patientId: String = "",
    val senderId: String,
    val senderRole: String,
    val senderName: String,
    val messageType: MessageType = MessageType.TEXT,
    val message: String? = null,
    val files: List<ConsultationFile> = emptyList(),
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = ConsultationMessage(
        id = id,
        consultationId = consultationId,
        doctorId = doctorId,
        patientId = patientId,
        senderId = senderId,
        senderRole = senderRole,
        senderName = senderName,
        messageType = messageType,
        message = message,
        files = files,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Serializable
enum class MessageType { TEXT, FILE, PRESCRIPTION }

@Serializable
data class ConsultationFile(
    val fileName: String,
    val url: String,
    val contentType: String,
    val sizeBytes: Long,
)

fun ConsultationMessage.toEntity() = ConsultationMessageEntity(
    id = id,
    consultationId = consultationId,
    doctorId = doctorId,
    patientId = patientId,
    senderId = senderId,
    senderRole = senderRole,
    senderName = senderName,
    messageType = messageType,
    message = message,
    files = files,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
