package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationMessagePageRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes

class GetConsultationHistoryUseCase(private val repository: ConsultationMessageRepository) {
    suspend operator fun invoke(
        consultationId: String,
        page: Int,
        size: Int,
    ): DefaultResponse<ConsultationMessagePageRes?> =
        repository.getByConsultationId(consultationId, page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                ConsultationMessagePageRes(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                )
            }
        }
}
