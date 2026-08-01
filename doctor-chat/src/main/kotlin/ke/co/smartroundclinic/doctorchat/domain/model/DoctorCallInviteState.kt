package ke.co.smartroundclinic.doctorchat.domain.model

import kotlinx.serialization.Serializable

/** Ephemeral ringing state for one doctor-to-doctor call invite, persisted in Redis with a TTL — mirrors consultation's CallInviteState but keyed by a single threadId instead of a (doctorId, patientId) pair. */
@Serializable
data class DoctorCallInviteState(
    val callId: String,
    val callerId: String,
    val calleeId: String,
    val threadId: String,
    val isVideo: Boolean,
    val createdAt: String,
)
