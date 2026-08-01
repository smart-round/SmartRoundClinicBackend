package ke.co.smartroundclinic.doctorchat.data.entity

import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.doctorchat.domain.model.DoctorChatMessage
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
enum class DoctorChatMessageType { TEXT, FILE }

@Serializable
data class DoctorChatFile(
    val fileName: String,
    val url: String,
    val contentType: String,
    val sizeBytes: Long,
)

data class DoctorChatMessageEntity(
    val id: String = ObjectId().toString(),
    val threadId: String,
    val senderId: String,
    val senderName: String,
    val messageType: DoctorChatMessageType = DoctorChatMessageType.TEXT,
    val message: String? = null,
    val files: List<DoctorChatFile> = emptyList(),
    val createdAt: String = sortableNowIso(),
) {
    fun toModel() = DoctorChatMessage(
        id = id,
        threadId = threadId,
        senderId = senderId,
        senderName = senderName,
        messageType = messageType,
        message = message,
        files = files,
        createdAt = createdAt,
    )
}
