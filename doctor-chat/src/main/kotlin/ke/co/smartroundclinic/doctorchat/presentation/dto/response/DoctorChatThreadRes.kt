package ke.co.smartroundclinic.doctorchat.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class DoctorChatThreadRes(
    val threadId: String,
    val counterpartId: String,
    val counterpartName: String,
    val counterpartPicture: String?,
    val lastMessagePreview: String?,
    val lastMessageAt: String?,
)
