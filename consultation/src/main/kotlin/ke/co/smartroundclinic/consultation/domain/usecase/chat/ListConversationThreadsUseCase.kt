package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConversationThreadRes

class ListConversationThreadsUseCase(
    private val sessionRepository: ConsultationSessionRepository,
    private val messageRepository: ConsultationMessageRepository,
) {
    suspend operator fun invoke(userId: String, role: String): DefaultResponse<List<ConversationThreadRes>?> {
        val result = sessionRepository.listThreadsForUser(userId, role)
        val isDoctor = role.equals("DOCTOR", ignoreCase = true)

        val mapped = (result as? Resource.Success)?.data?.map { session ->
            val counterpartId = if (isDoctor) session.patientId else session.doctorId
            val (counterpartName, counterpartPicture) = messageRepository.getUserInfo(counterpartId) ?: ("Unknown" to null)
            val latest = messageRepository.getLatestForThread(session.doctorId, session.patientId)

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
