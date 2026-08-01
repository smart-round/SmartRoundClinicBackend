package ke.co.smartroundclinic.doctorchat.presentation.dto.response

import kotlinx.serialization.Serializable

/**
 * Transient (non-persisted) WS events, mirroring consultation's typed-event family minus
 * TYPING/PRESENCE (not surfaced for doctor-doctor chat this round).
 */
@Serializable
data class DoctorCallInviteEventRes(
    val type: String = "CALL_INVITE",
    val callId: String,
    val callerId: String,
    val callerName: String?,
    val isVideo: Boolean,
    val ringTimeoutSeconds: Long,
)

@Serializable
data class DoctorCallAnsweredEventRes(val type: String = "CALL_ANSWERED", val callId: String)

@Serializable
data class DoctorCallDeclinedEventRes(val type: String = "CALL_DECLINED", val callId: String)

@Serializable
data class DoctorCallCancelledEventRes(val type: String = "CALL_CANCELLED", val callId: String)
