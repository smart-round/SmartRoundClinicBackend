package ke.co.smartroundclinic.support.domain.model

import ke.co.smartroundclinic.support.data.entity.TicketStatus

data class Ticket(
    val id: String,
    val ticketNumber: String,
    val issueCategoryId: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val complainantName: String,
    val complainantEmail: String,
    val status: TicketStatus,
    val assignedToId: String? = null,
    val assigneeName: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
)
