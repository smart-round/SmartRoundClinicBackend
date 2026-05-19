package ke.co.smartroundclinic.consultation.presentation.dto.response

import ke.co.smartroundclinic.consultation.data.entity.ConsultationStatus
import ke.co.smartroundclinic.consultation.domain.model.ConsultationSession
import kotlinx.serialization.Serializable

@Serializable
data class ConsultationSessionRes(
    val id: String,
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val status: ConsultationStatus,
    val videoRoomId: String?,
    val createdAt: String,
    val updatedAt: String?,
)

fun ConsultationSession.toRes() = ConsultationSessionRes(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    patientId = patientId,
    status = status,
    videoRoomId = videoRoomId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
