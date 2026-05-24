package ke.co.smartroundclinic.consultation.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class JoinCallRes(
    val meetingId: String,
    val participantId: String,
    val authToken: String,
    val presetName: String,
)
