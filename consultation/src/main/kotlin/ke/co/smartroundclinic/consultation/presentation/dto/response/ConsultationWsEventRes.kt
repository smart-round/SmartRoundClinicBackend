package ke.co.smartroundclinic.consultation.presentation.dto.response

import kotlinx.serialization.Serializable

/**
 * Transient (non-persisted) WebSocket events sent alongside [ConsultationMessageRes] frames on the
 * same `/consultation/{id}/chat` socket. Each carries its own top-level `type` so clients can peek
 * it first and dispatch to the right shape — everything else (absent, or "TEXT"/"FILE"/"PRESCRIPTION")
 * falls through to the existing [ConsultationMessageRes] decode path unchanged.
 */
@Serializable
data class ConsultationTypingEventRes(
    val type: String = "TYPING",
    val senderId: String,
    val isTyping: Boolean,
)

@Serializable
data class ConsultationPresenceEventRes(
    val type: String = "PRESENCE",
    val userId: String,
    val isOnline: Boolean,
    val lastSeenAt: String? = null,
)

@Serializable
data class ConsultationCallInviteEventRes(
    val type: String = "CALL_INVITE",
    val callId: String,
    val callerId: String,
    val callerName: String?,
    /** Pre-signed avatar URL, so the incoming-call screen can show who is calling. */
    val callerPicture: String? = null,
    val isVideo: Boolean,
    val ringTimeoutSeconds: Long,
    /** When this invite was created — lets the client detect a late-delivered/replayed signal and skip ringing on a call that's already timed out. */
    val createdAt: String,
)

@Serializable
data class ConsultationCallAnsweredEventRes(
    val type: String = "CALL_ANSWERED",
    val callId: String,
)

@Serializable
data class ConsultationCallDeclinedEventRes(
    val type: String = "CALL_DECLINED",
    val callId: String,
)

@Serializable
data class ConsultationCallCancelledEventRes(
    val type: String = "CALL_CANCELLED",
    val callId: String,
)
