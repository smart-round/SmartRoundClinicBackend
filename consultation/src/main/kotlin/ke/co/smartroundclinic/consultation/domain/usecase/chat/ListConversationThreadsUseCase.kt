package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConversationThreadRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.ThreadPreviewKind
import ke.co.smartroundclinic.infra.redis.RedisKeys
import ke.co.smartroundclinic.scheduling.data.entity.AppointmentEntity
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository

class ListConversationThreadsUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val messageRepository: ConsultationMessageRepository,
    private val hiddenThreadRepository: ConsultationHiddenThreadRepository,
    private val redis: RedisRepository,
) {
    suspend operator fun invoke(userId: String, role: String): DefaultResponse<List<ConversationThreadRes>?> {
        val isDoctor = role.equals("DOCTOR", ignoreCase = true)
        val result = if (isDoctor) appointmentRepository.getByDoctor(userId) else appointmentRepository.getByPatient(userId)
        val hiddenMap = (hiddenThreadRepository.getHiddenMap(userId) as? Resource.Success)?.data ?: emptyMap()

        // One permanent thread per (doctorId, patientId) pair that has ever had a real appointment
        // between them — that relationship, not any single visit, is what makes the thread exist.
        val latestAppointmentByPair: Map<Pair<String, String>, AppointmentEntity> = (result as? Resource.Success)?.data
            .orEmpty()
            .filter { it.status == "CONFIRMED" || it.status == "COMPLETED" }
            .groupBy { it.doctorId to it.patientId }
            .mapNotNull { (pair, appointments) -> appointments.maxByOrNull { it.bookedAt }?.let { pair to it } }
            .toMap()

        val mapped = latestAppointmentByPair.mapNotNull { (pair, latestAppointment) ->
            val (doctorId, patientId) = pair
            val latest = messageRepository.getLatestForThread(doctorId, patientId)

            // Stays hidden only if never un-hidden by a newer message arriving after the hide —
            // a thread with no messages at all can't have had one arrive, so it stays hidden once hidden.
            val hiddenAt = hiddenMap[doctorId to patientId]
            val stillHidden = hiddenAt != null && (latest?.createdAt == null || hiddenAt >= latest.createdAt)
            if (stillHidden) return@mapNotNull null

            val counterpartId = if (isDoctor) patientId else doctorId
            val (counterpartName, counterpartPicture) = messageRepository.getUserInfo(counterpartId) ?: ("Unknown" to null)
            val isOnline = redis.get(RedisKeys.presence(counterpartId)) == "true"
            val lastSeenAt = if (isOnline) null else messageRepository.getLastSeenAt(counterpartId)

            ConversationThreadRes(
                threadId = "$doctorId:$patientId",
                doctorId = doctorId,
                patientId = patientId,
                counterpartName = counterpartName,
                counterpartPicture = counterpartPicture,
                lastMessagePreview = latest?.let(::previewOf),
                lastMessageKind = latest?.let(::kindOf) ?: ThreadPreviewKind.TEXT,
                lastMessageAt = latest?.createdAt,
                latestAppointmentId = latestAppointment.id,
                isOnline = isOnline,
                lastSeenAt = lastSeenAt,
            )
        }.sortedByDescending { it.lastMessageAt ?: "" }

        return result.toDefaultResponse { mapped }
    }

    /**
     * Chat-list preview text. Images deliberately never show their filename: a camera capture
     * is named with a generated id and a gallery pick with something like "xray.png", neither
     * of which reads as anything in a thread list. Both become "Photo".
     */
    private fun previewOf(message: ConsultationMessageEntity): String =
        when (message.messageType) {
            MessageType.TEXT -> message.message ?: ""
            MessageType.PRESCRIPTION -> "Prescription"
            MessageType.FILE -> {
                val file = message.files.firstOrNull()
                when {
                    file == null -> "Attachment"
                    file.isImage() -> "Photo"
                    else -> file.fileName
                }
            }
        }

    private fun kindOf(message: ConsultationMessageEntity): ThreadPreviewKind =
        when (message.messageType) {
            MessageType.TEXT -> ThreadPreviewKind.TEXT
            MessageType.PRESCRIPTION -> ThreadPreviewKind.PRESCRIPTION
            MessageType.FILE ->
                if (message.files.firstOrNull()?.isImage() == true) ThreadPreviewKind.PHOTO
                else ThreadPreviewKind.FILE
        }

    /** contentType is authoritative; the extension is a fallback for older records. */
    private fun ConsultationFile.isImage(): Boolean =
        contentType.startsWith("image/", ignoreCase = true) ||
            fileName.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

    private companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp")
    }
}
