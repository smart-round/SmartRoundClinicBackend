package ke.co.smartroundclinic.consultation.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationSessionEntity

interface ConsultationSessionRepository {
    suspend fun startOrGet(appointmentId: String, userId: String): Resource<ConsultationSessionEntity>
    suspend fun getById(id: String): Resource<ConsultationSessionEntity?>
    suspend fun getByAppointmentId(appointmentId: String): Resource<ConsultationSessionEntity?>
    suspend fun setVideoRoomId(id: String, videoRoomId: String): Resource<ConsultationSessionEntity?>
    suspend fun clearVideoRoomId(id: String): Resource<Unit>
    suspend fun end(id: String, doctorId: String): Resource<ConsultationSessionEntity?>
}
