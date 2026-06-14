package ke.co.smartroundclinic.support.domain.usecase.ticket

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.support.data.entity.TicketStatus
import ke.co.smartroundclinic.support.domain.repository.TicketRepository
import ke.co.smartroundclinic.support.presentation.dto.response.TicketRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes

class UpdateTicketStatusUseCase(
    private val repository: TicketRepository,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(id: String, status: TicketStatus): DefaultResponse<TicketRes?> {
        val result = repository.updateStatus(id, status)
        if (result is Resource.Success) {
            val ticket = result.data
            if (ticket?.complainantId != null) {
                runCatching {
                    notificationSender?.send(
                        title = "Support ticket status updated",
                        message = "Your ticket \"${ticket.title}\" is now ${status.label()}",
                        channel = NotificationChannel.PUSH_NOTIFICATION,
                        destination = NotificationDestination.ALL,
                        recipientId = ticket.complainantId,
                    )
                }
            }
        }
        return result.toDefaultResponse(failedStatusCode = 404) { it?.toModel("")?.toRes() }
    }

    private fun TicketStatus.label() = when (this) {
        TicketStatus.OPEN -> "Open"
        TicketStatus.IN_PROGRESS -> "In Progress"
        TicketStatus.CLOSED -> "Closed"
    }
}
