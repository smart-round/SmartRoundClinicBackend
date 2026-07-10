package ke.co.smartroundclinic.consultation.data.entity

import ke.co.smartroundclinic.common.sortableNowIso
import org.bson.types.ObjectId

/**
 * One doc per permanent (doctorId, patientId) thread — the call-room bookkeeping for that pair.
 * Chat messages themselves don't need this row at all (they're just doctorId/patientId-tagged);
 * this exists purely so a video/voice call has somewhere to remember its current/last meeting id.
 */
data class ConsultationThreadEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val patientId: String,
    val videoRoomId: String? = null,
    val lastVideoRoomId: String? = null,
    val createdAt: String = sortableNowIso(),
    val updatedAt: String? = null,
)
