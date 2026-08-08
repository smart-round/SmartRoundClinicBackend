package ke.co.smartroundclinic.doctorchat.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class DoctorChatThreadRes(
    val threadId: String,
    val counterpartId: String,
    val counterpartName: String,
    val counterpartPicture: String?,
    val lastMessagePreview: String?,
    val lastMessageAt: String?,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
    /** See [ThreadPreviewKind] — lets clients pick an icon without parsing the preview text. */
    val lastMessageKind: ThreadPreviewKind = ThreadPreviewKind.TEXT,
)

/**
 * Deliberately duplicated from consultation's enum of the same name rather than shared:
 * doctor-chat does not depend on consultation, and the wire values are identical.
 */
@Serializable
enum class ThreadPreviewKind { TEXT, PHOTO, VIDEO, FILE, PRESCRIPTION }
