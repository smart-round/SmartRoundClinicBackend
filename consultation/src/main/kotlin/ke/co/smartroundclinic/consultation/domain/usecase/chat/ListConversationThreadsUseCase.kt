package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConversationThreadRes
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
                lastMessageAt = latest?.createdAt,
                latestAppointmentId = latestAppointment.id,
                isOnline = isOnline,
                lastSeenAt = lastSeenAt,
            )
        }.sortedByDescending { it.lastMessageAt ?: "" }

        return result.toDefaultResponse { mapped }
    }

    private fun previewOf(message: ConsultationMessageEntity): String =
        when (message.messageType) {
            MessageType.TEXT -> message.message ?: ""
            MessageType.FILE -> message.files.firstOrNull()?.fileName ?: "Attachment"
            MessageType.PRESCRIPTION -> "Prescription"
        }
}
