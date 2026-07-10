package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConversationThreadRes
import ke.co.smartroundclinic.infra.redis.RedisKeys

class ListConversationThreadsUseCase(
    private val sessionRepository: ConsultationSessionRepository,
    private val messageRepository: ConsultationMessageRepository,
    private val hiddenThreadRepository: ConsultationHiddenThreadRepository,
    private val redis: RedisRepository,
) {
    suspend operator fun invoke(userId: String, role: String): DefaultResponse<List<ConversationThreadRes>?> {
        val result = sessionRepository.listThreadsForUser(userId, role)
        val isDoctor = role.equals("DOCTOR", ignoreCase = true)
        val hiddenMap = (hiddenThreadRepository.getHiddenMap(userId) as? Resource.Success)?.data ?: emptyMap()

        val mapped = (result as? Resource.Success)?.data?.mapNotNull { session ->
            val latest = messageRepository.getLatestForThread(session.doctorId, session.patientId)

            // Stays hidden only if never un-hidden by a newer message arriving after the hide —
            // a thread with no messages at all can't have had one arrive, so it stays hidden once hidden.
            val hiddenAt = hiddenMap[session.doctorId to session.patientId]
            val stillHidden = hiddenAt != null && (latest?.createdAt == null || hiddenAt >= latest.createdAt)
            if (stillHidden) return@mapNotNull null

            val counterpartId = if (isDoctor) session.patientId else session.doctorId
            val (counterpartName, counterpartPicture) = messageRepository.getUserInfo(counterpartId) ?: ("Unknown" to null)
            val isOnline = redis.get(RedisKeys.presence(counterpartId)) == "true"
            val lastSeenAt = if (isOnline) null else messageRepository.getLastSeenAt(counterpartId)

            ConversationThreadRes(
                threadId = "${session.doctorId}:${session.patientId}",
                doctorId = session.doctorId,
                patientId = session.patientId,
                counterpartName = counterpartName,
                counterpartPicture = counterpartPicture,
                lastMessagePreview = latest?.let(::previewOf),
                lastMessageAt = latest?.createdAt,
                latestConsultationStatus = session.status.name,
                latestAppointmentId = session.appointmentId,
                isOnline = isOnline,
                lastSeenAt = lastSeenAt,
            )
        }?.sortedByDescending { it.lastMessageAt ?: "" }

        return result.toDefaultResponse { mapped }
    }

    private fun previewOf(message: ConsultationMessageEntity): String =
        when (message.messageType) {
            MessageType.TEXT -> message.message ?: ""
            MessageType.FILE -> message.files.firstOrNull()?.fileName ?: "Attachment"
            MessageType.PRESCRIPTION -> "Prescription"
        }
}
