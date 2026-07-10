package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository

class HideConversationThreadUseCase(
    private val repository: ConsultationHiddenThreadRepository,
) {
    suspend operator fun invoke(userId: String, doctorId: String, patientId: String): DefaultResponse<Unit?> =
        repository.hide(userId, doctorId, patientId).toDefaultResponse { it }
}
