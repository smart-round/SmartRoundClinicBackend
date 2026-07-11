package ke.co.smartroundclinic.consultation.domain.model

import kotlinx.serialization.Serializable

/** Ephemeral ringing state for one call invite, persisted in Redis with a TTL (see RedisKeys.CALL_INVITE_TTL_SECONDS). */
@Serializable
data class CallInviteState(
    val callId: String,
    val callerId: String,
    val calleeId: String,
    val doctorId: String,
    val patientId: String,
    val isVideo: Boolean,
    val createdAt: String,
)
