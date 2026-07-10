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
data class ConsultationReadReceiptEventRes(
    val type: String = "READ",
    val doctorId: String,
    val patientId: String,
    val readerId: String,
    val lastReadAt: String,
)
