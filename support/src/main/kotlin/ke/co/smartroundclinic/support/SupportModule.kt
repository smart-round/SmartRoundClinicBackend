package ke.co.smartroundclinic.support

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.support.domain.service.IssueCategoryService
import ke.co.smartroundclinic.support.domain.service.SupportTicketChatService
import ke.co.smartroundclinic.support.domain.service.TicketService
import ke.co.smartroundclinic.support.presentation.controller.issueCategoryController
import ke.co.smartroundclinic.support.presentation.controller.supportTicketChatController
import ke.co.smartroundclinic.support.presentation.controller.ticketController
import org.koin.ktor.ext.inject

fun Application.supportModule() {
    val issueCategoryService: IssueCategoryService by inject()
    val ticketService: TicketService by inject()
    val chatService: SupportTicketChatService by inject()
    routing {
        issueCategoryController(issueCategoryService)
        ticketController(ticketService)
        supportTicketChatController(chatService)
    }
}

