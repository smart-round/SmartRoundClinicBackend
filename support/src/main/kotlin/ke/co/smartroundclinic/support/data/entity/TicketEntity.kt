package ke.co.smartroundclinic.support.data.entity

import ke.co.smartroundclinic.support.domain.model.Ticket
import org.bson.types.ObjectId
import kotlin.time.Clock

data class TicketEntity(
    val id: String = ObjectId().toString(),
    val ticketNumber: String,
    val issueCategoryId: String,
    val title: String,
    val description: String,
    val complainantName: String,
    val complainantEmail: String,
    val complainantId: String? = null,
    val status: TicketStatus = TicketStatus.OPEN,
    val assignedToId: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel(categoryName: String, assigneeName: String? = null) = Ticket(
        id = id,
        ticketNumber = ticketNumber,
        issueCategoryId = issueCategoryId,
        categoryName = categoryName,
        title = title,
        description = description,
        complainantName = complainantName,
        complainantEmail = complainantEmail,
        complainantId = complainantId,
        status = status,
        assignedToId = assignedToId,
        assigneeName = assigneeName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

enum class TicketStatus { OPEN, IN_PROGRESS, CLOSED }

fun Ticket.toEntity() = TicketEntity(
    id = id,
    ticketNumber = ticketNumber,
    issueCategoryId = issueCategoryId,
    title = title,
    description = description,
    complainantName = complainantName,
    complainantEmail = complainantEmail,
    complainantId = complainantId,
    status = status,
    assignedToId = assignedToId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
