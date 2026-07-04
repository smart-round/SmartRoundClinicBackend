package ke.co.smartroundclinic.support.domain.usecase.ticket

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import ke.co.smartroundclinic.support.data.entity.TicketEntity
import ke.co.smartroundclinic.support.domain.repository.IssueCategoryRepository
import org.slf4j.LoggerFactory

class SendSupportTicketEmailUseCase(
    private val emailRepository: EmailRepository,
    private val emailConfig: EmailConfig,
    private val issueCategoryRepository: IssueCategoryRepository,
) {
    private val logger = LoggerFactory.getLogger(SendSupportTicketEmailUseCase::class.java)

    suspend fun onCreated(entity: TicketEntity) = send(
        entity = entity,
        eventTitle = "New Support Ticket",
        eventMessage = "A new support ticket has been submitted and requires triage.",
    )

    suspend fun onStatusChanged(entity: TicketEntity) = send(
        entity = entity,
        eventTitle = "Ticket Status Updated",
        eventMessage = "A support ticket status has been updated and may need attention.",
    )

    private suspend fun send(entity: TicketEntity, eventTitle: String, eventMessage: String) {
        val categoryName = when (val res = issueCategoryRepository.getById(entity.issueCategoryId)) {
            is Resource.Success -> res.data?.name ?: "Unknown"
            is Resource.Error -> "Unknown"
        }
        runCatching {
            emailRepository.sendEmailWithTemplate(
                EmailWithTemplate(
                    from = emailConfig.accountSupportEmail,
                    to = emailConfig.accountSupportMailboxDestination,
                    template = Template(
                        id = emailConfig.accountSupportTemplateId,
                        variables = mapOf(
                            "EVENT_TITLE" to eventTitle,
                            "EVENT_MESSAGE" to eventMessage,
                            "TICKET_NUMBER" to entity.ticketNumber,
                            "TICKET_ID" to entity.id,
                            "STATUS" to entity.status.name,
                            "ISSUE_CATEGORY_NAME" to categoryName,
                            "TITLE" to entity.title,
                            "COMPLAINANT_NAME" to entity.complainantName,
                            "COMPLAINANT_EMAIL" to entity.complainantEmail,
                            "DESCRIPTION" to entity.description,
                        ),
                    ),
                )
            )
        }.onFailure { e ->
            logger.error(
                "Failed to send support ticket email " +
                    "[templateId=${emailConfig.accountSupportTemplateId}, " +
                    "to=${emailConfig.accountSupportMailboxDestination}, event=$eventTitle]",
                e,
            )
        }
    }
}
