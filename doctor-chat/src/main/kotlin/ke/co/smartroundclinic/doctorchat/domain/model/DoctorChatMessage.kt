package ke.co.smartroundclinic.doctorchat.domain.model

import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatFile
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatMessageType

data class DoctorChatMessage(
    val id: String,
    val threadId: String,
    val senderId: String,
    val senderName: String,
    val messageType: DoctorChatMessageType,
    val message: String?,
    val files: List<DoctorChatFile>,
    val createdAt: String,
)
