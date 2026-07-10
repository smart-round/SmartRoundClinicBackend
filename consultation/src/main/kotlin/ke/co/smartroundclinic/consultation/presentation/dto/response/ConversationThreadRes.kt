package ke.co.smartroundclinic.consultation.presentation.dto.response

import kotlinx.serialization.Serializable

/**
 * One row in the doctor/patient chat list — one entry per (doctorId, patientId) pair,
 * a single permanent conversation independent of any particular appointment/visit.
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
    val latestAppointmentId: String,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
)
