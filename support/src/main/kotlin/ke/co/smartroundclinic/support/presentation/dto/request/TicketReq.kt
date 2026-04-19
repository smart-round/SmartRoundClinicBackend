package ke.co.smartroundclinic.support.presentation.dto.request

import ke.co.smartroundclinic.support.data.entity.TicketStatus
import ke.co.smartroundclinic.support.domain.model.Ticket
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class CreateTicketReq(
    val issueCategoryId: String,
    val title: String,
    val description: String,
    val complainantName: String,
    val complainantEmail: String,
) {
    fun toModel() = Ticket(
        id = ObjectId().toString(),
        ticketNumber = generateTicketNumber(),
        issueCategoryId = issueCategoryId,
        categoryName = "",
        title = title,
        description = description,
        complainantName = complainantName,
        complainantEmail = complainantEmail,
        status = TicketStatus.OPEN,
        assignedToId = null,
        assigneeName = null,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}

private fun generateTicketNumber(): String {
    val digits = (1000000..9999999).random()
    val letters = (1..3).map { ('A'..'Z').random() }.joinToString("")
    return "#$digits$letters"
}

@Serializable
data class UpdateTicketStatusReq(val status: TicketStatus)

@Serializable
data class AssignTicketReq(val adminUserId: String)
