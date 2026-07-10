package ke.co.smartroundclinic.consultation.presentation.dto.response

import kotlinx.serialization.Serializable

/**
 * One row in the doctor/patient chat list — one entry per (doctorId, patientId) pair,
 * merging all of that pair's consultations into a single conversation.
 */
@Serializable
data class ConversationThreadRes(
    val threadId: String,
    val doctorId: String,
    val patientId: String,
    val counterpartName: String,
    val counterpartPicture: String?,
    val lastMessagePreview: String?,
    val lastMessageAt: String?,
    val latestConsultationStatus: String,
    val latestAppointmentId: String,
)
