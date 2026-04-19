package ke.co.smartroundclinic.support.domain.usecase.ticket

import ke.co.smartroundclinic.support.data.entity.TicketStatus
import ke.co.smartroundclinic.support.domain.repository.TicketRepository
import ke.co.smartroundclinic.support.presentation.dto.response.TicketRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateTicketStatusUseCase(private val repository: TicketRepository) {
    suspend operator fun invoke(id: String, status: TicketStatus): DefaultResponse<TicketRes?> =
        repository.updateStatus(id, status)
            .toDefaultResponse(failedStatusCode = 404) { it?.toModel("")?.toRes() }
}
