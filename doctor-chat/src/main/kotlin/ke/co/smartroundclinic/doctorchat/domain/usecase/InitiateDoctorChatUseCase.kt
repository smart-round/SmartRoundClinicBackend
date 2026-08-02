package ke.co.smartroundclinic.doctorchat.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.VerifiedDoctorResolver
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorChatThreadRes

/** "Connect" — finds or creates the permanent thread between two doctors, gated on both being verified. */
class InitiateDoctorChatUseCase(
    private val threads: DoctorChatThreadRepository,
    private val messages: DoctorChatMessageRepository,
    private val verifiedDoctorResolver: VerifiedDoctorResolver? = null,
) {
    suspend operator fun invoke(callerId: String, otherDoctorId: String): DefaultResponse<DoctorChatThreadRes?> {
        if (callerId == otherDoctorId) {
            return Resource.Error<Nothing>("You cannot start a chat with yourself").toDefaultResponse(failedStatusCode = 400) { null }
        }

        if (verifiedDoctorResolver != null) {
            if (!verifiedDoctorResolver.isVerified(callerId) || !verifiedDoctorResolver.isVerified(otherDoctorId)) {
                return Resource.Error<Nothing>("Both doctors must be verified to start a chat")
                    .toDefaultResponse(failedStatusCode = 403) { null }
            }
        }

        val result = threads.getOrCreate(callerId, otherDoctorId)
        val (counterpartName, counterpartPicture) = messages.getUserInfo(otherDoctorId) ?: ("Unknown" to null)
        return result.toDefaultResponse { thread ->
            thread?.let {
                DoctorChatThreadRes(
                    threadId = it.id,
                    counterpartId = otherDoctorId,
                    counterpartName = counterpartName,
                    counterpartPicture = counterpartPicture,
                    lastMessagePreview = null,
                    lastMessageAt = null,
                )
            }
        }
    }
}
