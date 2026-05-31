package ke.co.smartroundclinic.support.domain.usecase.ticket

import ke.co.smartroundclinic.support.data.entity.toEntity
import ke.co.smartroundclinic.support.domain.repository.TicketRepository
import ke.co.smartroundclinic.support.presentation.dto.request.CreateTicketReq
import ke.co.smartroundclinic.support.presentation.dto.response.TicketRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateTicketUseCase(private val repository: TicketRepository) {
    suspend operator fun invoke(req: CreateTicketReq, complainantId: String): DefaultResponse<TicketRes?> =
        repository.create(req.toModel().toEntity().copy(complainantId = complainantId))
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { it?.toModel("")?.toRes() }
}
