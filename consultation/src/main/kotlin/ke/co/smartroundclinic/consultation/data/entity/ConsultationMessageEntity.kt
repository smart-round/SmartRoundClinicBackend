package ke.co.smartroundclinic.consultation.data.entity

import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.consultation.domain.model.ConsultationMessage
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

data class ConsultationMessageEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val patientId: String,
    val senderId: String,
    val senderRole: String,
    val senderName: String,
    val messageType: MessageType = MessageType.TEXT,
    val message: String? = null,
    val files: List<ConsultationFile> = emptyList(),
    // Set only for PRESCRIPTION messages — which visit they were issued for. Plain chat messages
    // belong to the permanent (doctorId, patientId) thread, not any particular visit.
    val appointmentId: String? = null,
    val createdAt: String = sortableNowIso(),
    val updatedAt: String? = null,
) {
    fun toModel() = ConsultationMessage(
        id = id,
        doctorId = doctorId,
        patientId = patientId,
        senderId = senderId,
        senderRole = senderRole,
        senderName = senderName,
        messageType = messageType,
        message = message,
        files = files,
        appointmentId = appointmentId,
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
    doctorId = doctorId,
    patientId = patientId,
    senderId = senderId,
    senderRole = senderRole,
    senderName = senderName,
    messageType = messageType,
    message = message,
    files = files,
    appointmentId = appointmentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
