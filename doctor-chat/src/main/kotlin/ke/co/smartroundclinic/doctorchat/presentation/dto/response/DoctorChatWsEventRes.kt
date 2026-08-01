package ke.co.smartroundclinic.doctorchat.presentation.dto.response

import kotlinx.serialization.Serializable

/** Transient (non-persisted) WS events, mirroring consultation's typed-event family. */
@Serializable
data class DoctorTypingEventRes(
    val type: String = "TYPING",
    val senderId: String,
    val isTyping: Boolean,
)

@Serializable
data class DoctorPresenceEventRes(
    val type: String = "PRESENCE",
    val userId: String,
    val isOnline: Boolean,
    val lastSeenAt: String? = null,
)

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
