package ke.co.smartroundclinic.doctorchat.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class JoinDoctorCallRes(
    val meetingId: String,
    val participantId: String,
    val authToken: String,
    val presetName: String,
)

@Serializable
data class InviteToDoctorCallRes(
    val callId: String,
    val ringTimeoutSeconds: Long,
)
