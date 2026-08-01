package ke.co.smartroundclinic.doctorchat.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class InitiateDoctorChatReq(val otherDoctorId: String)

/** WS text-frame envelope. Same shape as consultation's, minus the file fields this module doesn't use. */
@Serializable
data class DoctorChatWsMessage(
    val type: String,
    val message: String? = null,
    val isTyping: Boolean? = null,
)

@Serializable
data class InviteToDoctorCallReq(val isVideo: Boolean = true)

@Serializable
data class DoctorCallActionReq(val callId: String)
