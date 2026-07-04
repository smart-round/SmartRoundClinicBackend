package ke.co.smartroundclinic.support.domain.usecase.ticket

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.support.data.entity.toEntity
import ke.co.smartroundclinic.support.domain.repository.TicketRepository
import ke.co.smartroundclinic.support.presentation.dto.request.CreateTicketReq
import ke.co.smartroundclinic.support.presentation.dto.response.TicketRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class CreateTicketUseCase(
    private val repository: TicketRepository,
    private val sendTicketEmail: SendSupportTicketEmailUseCase? = null,
) {
    suspend operator fun invoke(req: CreateTicketReq, complainantId: String): DefaultResponse<TicketRes?> =
        supervisorScope {
            val result = repository.create(req.toModel().toEntity().copy(complainantId = complainantId))
            if (result is Resource.Success) {
                val entity = result.data
                if (entity != null) launch { sendTicketEmail?.onCreated(entity) }
            }
            result.toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { it?.toModel("")?.toRes() }
        }
}
