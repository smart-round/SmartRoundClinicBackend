package ke.co.smartroundclinic.doctorchat.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatFile
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatMessageType
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorChatThreadRes
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.ThreadPreviewKind
import ke.co.smartroundclinic.infra.redis.RedisKeys

class GetMyDoctorChatThreadsUseCase(
    private val threads: DoctorChatThreadRepository,
    private val messages: DoctorChatMessageRepository,
    private val redis: RedisRepository,
) {
    suspend operator fun invoke(callerId: String): DefaultResponse<List<DoctorChatThreadRes>?> {
        val result = threads.getThreadsForDoctor(callerId)
        val entities = result.data ?: emptyList()

        val mapped = entities.map { thread ->
            val counterpartId = if (thread.doctorAId == callerId) thread.doctorBId else thread.doctorAId
            val (counterpartName, counterpartPicture) = messages.getUserInfo(counterpartId) ?: ("Unknown" to null)
            val latest = messages.getLatestForThread(thread.id)
            val isOnline = redis.get(RedisKeys.presence(counterpartId)) == "true"
            val lastSeenAt = if (isOnline) null else messages.getLastSeenAt(counterpartId)
            DoctorChatThreadRes(
                threadId = thread.id,
                counterpartId = counterpartId,
                counterpartName = counterpartName,
                counterpartPicture = counterpartPicture,
                lastMessagePreview = latest?.let {
                    when (it.messageType) {
                        DoctorChatMessageType.TEXT -> it.message ?: ""
                        DoctorChatMessageType.FILE -> {
                            val file = it.files.firstOrNull()
                            when {
                                file == null -> "Attachment"
                                file.isImage() -> "Photo"
                                file.isVideo() -> "Video"
                                file.isDocument() -> file.fileName
                                else -> "File"
                            }
                        }
                    }
                },
                lastMessageKind = latest?.let {
                    when (it.messageType) {
                        DoctorChatMessageType.TEXT -> ThreadPreviewKind.TEXT
                        DoctorChatMessageType.FILE -> {
                            val file = it.files.firstOrNull()
                            when {
                                file?.isImage() == true -> ThreadPreviewKind.PHOTO
                                file?.isVideo() == true -> ThreadPreviewKind.VIDEO
                                else -> ThreadPreviewKind.FILE
                            }
                        }
                    }
                } ?: ThreadPreviewKind.TEXT,
                lastMessageAt = latest?.createdAt,
                isOnline = isOnline,
                lastSeenAt = lastSeenAt,
            )
        }.sortedByDescending { it.lastMessageAt ?: "" }

        return result.toDefaultResponse { mapped }
    }

    /** contentType is authoritative; the extension is a fallback for older records. */
    private fun DoctorChatFile.isImage(): Boolean =
        contentType.startsWith("image/", ignoreCase = true) || ext() in IMAGE_EXTENSIONS

    private fun DoctorChatFile.isVideo(): Boolean =
        contentType.startsWith("video/", ignoreCase = true) || ext() in VIDEO_EXTENSIONS

    private fun DoctorChatFile.isDocument(): Boolean =
        contentType == "application/pdf" || ext() in DOCUMENT_EXTENSIONS

    private fun DoctorChatFile.ext(): String = fileName.substringAfterLast('.', "").lowercase()

    private companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp")
        val VIDEO_EXTENSIONS = setOf("mp4", "mov", "avi", "mkv", "webm", "3gp", "m4v", "mpeg", "mpg")
        val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "csv", "ppt", "pptx", "txt", "rtf")
    }
}
