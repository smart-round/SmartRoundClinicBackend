package ke.co.smartroundclinic.consultation.data.entity

import ke.co.smartroundclinic.common.sortableNowIso
import ke.co.smartroundclinic.consultation.domain.model.ConsultationSession
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

data class ConsultationSessionEntity(
    val id: String = ObjectId().toString(),
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val status: ConsultationStatus = ConsultationStatus.ACTIVE,
    val videoRoomId: String? = null,
    val lastVideoRoomId: String? = null,
    val createdAt: String = sortableNowIso(),
    val updatedAt: String? = null,
) {
    fun toModel() = ConsultationSession(
        id = id,
        appointmentId = appointmentId,
        doctorId = doctorId,
        patientId = patientId,
        status = status,
        videoRoomId = videoRoomId,
        lastVideoRoomId = lastVideoRoomId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Serializable
enum class ConsultationStatus { ACTIVE, ENDED }

fun ConsultationSession.toEntity() = ConsultationSessionEntity(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    patientId = patientId,
    status = status,
    videoRoomId = videoRoomId,
    lastVideoRoomId = lastVideoRoomId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
