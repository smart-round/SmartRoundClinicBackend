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
    /**
     * What the last message *was*, so clients can pick an icon without string-matching the
     * preview text. Defaults to TEXT for older clients that ignore it.
     */
    val lastMessageKind: ThreadPreviewKind = ThreadPreviewKind.TEXT,
)

@Serializable
enum class ThreadPreviewKind { TEXT, PHOTO, VIDEO, FILE, PRESCRIPTION }
