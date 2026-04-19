package ke.co.smartroundclinic.support.presentation.dto.response

import ke.co.smartroundclinic.support.data.entity.TicketStatus
import ke.co.smartroundclinic.support.domain.model.Ticket
import kotlinx.serialization.Serializable

@Serializable
data class TicketRes(
    val id: String,
    val ticketNumber: String,
    val issueCategoryId: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val complainantName: String,
    val complainantEmail: String,
    val status: TicketStatus,
    val assignedToId: String?,
    val assigneeName: String?,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class TicketPageResult(
    val items: List<TicketRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun Ticket.toRes() = TicketRes(
    id = id,
    ticketNumber = ticketNumber,
    issueCategoryId = issueCategoryId,
    categoryName = categoryName,
    title = title,
    description = description,
    complainantName = complainantName,
    complainantEmail = complainantEmail,
    status = status,
    assignedToId = assignedToId,
    assigneeName = assigneeName,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
