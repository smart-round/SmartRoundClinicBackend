package ke.co.smartroundclinic.consultation.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationSessionEntity

interface ConsultationSessionRepository {
    suspend fun startOrGet(appointmentId: String, userId: String): Resource<ConsultationSessionEntity>
    suspend fun getById(id: String): Resource<ConsultationSessionEntity?>
    suspend fun getByAppointmentId(appointmentId: String): Resource<ConsultationSessionEntity?>
    suspend fun getByVideoRoomId(videoRoomId: String): Resource<ConsultationSessionEntity?>
    suspend fun setVideoRoomId(id: String, videoRoomId: String): Resource<ConsultationSessionEntity?>
    /** Stores [videoRoomId] only if the session has no meeting ID yet.
     *  Returns the meeting ID that is actually stored after the call —
     *  either [videoRoomId] (this call won) or the existing one (another request beat us). */
    suspend fun setVideoRoomIdIfAbsent(id: String, videoRoomId: String): Resource<String>
    suspend fun clearVideoRoomId(id: String): Resource<Unit>
    suspend fun end(id: String, doctorId: String): Resource<ConsultationSessionEntity?>
}
