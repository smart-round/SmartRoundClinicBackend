package ke.co.smartroundclinic.consultation.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class InviteToCallRes(
    val callId: String,
    val ringTimeoutSeconds: Long,
)
