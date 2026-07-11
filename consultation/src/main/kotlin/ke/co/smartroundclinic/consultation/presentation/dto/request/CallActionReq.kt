package ke.co.smartroundclinic.consultation.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class InviteToCallReq(
    val isVideo: Boolean = true,
)

@Serializable
data class CallActionReq(
    val callId: String,
)
