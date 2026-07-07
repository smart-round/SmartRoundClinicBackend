package ke.co.smartroundclinic.consultation.domain.model

import ke.co.smartroundclinic.consultation.data.entity.ConsultationStatus

data class ConsultationSession(
    val id: String,
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val status: ConsultationStatus,
    val videoRoomId: String?,
    val lastVideoRoomId: String?,
    val createdAt: String,
    val updatedAt: String?,
)
