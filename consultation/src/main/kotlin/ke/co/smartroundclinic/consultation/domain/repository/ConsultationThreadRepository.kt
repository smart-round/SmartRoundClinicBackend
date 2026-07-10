package ke.co.smartroundclinic.consultation.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationThreadEntity

interface ConsultationThreadRepository {
    /** Returns the thread for this pair, creating an empty one if it doesn't exist yet. */
    suspend fun getOrCreate(doctorId: String, patientId: String): Resource<ConsultationThreadEntity>
    suspend fun getByVideoRoomId(videoRoomId: String): Resource<ConsultationThreadEntity?>
    suspend fun setVideoRoomId(doctorId: String, patientId: String, videoRoomId: String): Resource<ConsultationThreadEntity?>

    /** Stores [videoRoomId] only if the thread has no meeting ID yet.
     *  Returns the meeting ID that is actually stored after the call —
     *  either [videoRoomId] (this call won) or the existing one (another request beat us). */
    suspend fun setVideoRoomIdIfAbsent(doctorId: String, patientId: String, videoRoomId: String): Resource<String>
    suspend fun clearVideoRoomId(doctorId: String, patientId: String, completedRoomId: String? = null): Resource<Unit>
}
