package ke.co.smartroundclinic.support.domain.service

import ke.co.smartroundclinic.support.data.entity.TicketStatus
import ke.co.smartroundclinic.support.domain.usecase.ticket.AssignTicketUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.CreateTicketUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.DeleteTicketUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.GetAllTicketsUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.GetTicketByIdUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.UpdateTicketStatusUseCase
import ke.co.smartroundclinic.support.presentation.dto.request.CreateTicketReq

class TicketService(
    private val createUseCase: CreateTicketUseCase,
    private val getByIdUseCase: GetTicketByIdUseCase,
    private val getAllUseCase: GetAllTicketsUseCase,
    private val updateStatusUseCase: UpdateTicketStatusUseCase,
    private val assignUseCase: AssignTicketUseCase,
    private val deleteUseCase: DeleteTicketUseCase,
) {
    suspend fun create(req: CreateTicketReq) = createUseCase(req)
    suspend fun getById(id: String) = getByIdUseCase(id)
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun updateStatus(id: String, status: TicketStatus) = updateStatusUseCase(id, status)
    suspend fun assign(id: String, adminUserId: String) = assignUseCase(id, adminUserId)
    suspend fun delete(id: String) = deleteUseCase(id)
}
