package ke.co.smartroundclinic.doctorchat.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatMessageType
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorChatThreadRes

class GetMyDoctorChatThreadsUseCase(
    private val threads: DoctorChatThreadRepository,
    private val messages: DoctorChatMessageRepository,
) {
    suspend operator fun invoke(callerId: String): DefaultResponse<List<DoctorChatThreadRes>?> {
        val result = threads.getThreadsForDoctor(callerId)
        val entities = result.data ?: emptyList()

        val mapped = entities.map { thread ->
            val counterpartId = if (thread.doctorAId == callerId) thread.doctorBId else thread.doctorAId
            val (counterpartName, counterpartPicture) = messages.getUserInfo(counterpartId) ?: ("Unknown" to null)
            val latest = messages.getLatestForThread(thread.id)
            DoctorChatThreadRes(
                threadId = thread.id,
                counterpartId = counterpartId,
                counterpartName = counterpartName,
                counterpartPicture = counterpartPicture,
                lastMessagePreview = latest?.let {
                    when (it.messageType) {
                        DoctorChatMessageType.TEXT -> it.message ?: ""
                        DoctorChatMessageType.FILE -> it.files.firstOrNull()?.fileName ?: "Attachment"
                    }
                },
                lastMessageAt = latest?.createdAt,
            )
        }.sortedByDescending { it.lastMessageAt ?: "" }

        return result.toDefaultResponse { mapped }
    }
}
