package ke.co.smartroundclinic.consultation

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndThreadCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.HandleMeetingEndedWebhookUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinThreadCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetMergedConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.HideConversationThreadUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.ListConversationThreadsUseCase
import ke.co.smartroundclinic.consultation.presentation.controller.consultationChatController
import ke.co.smartroundclinic.consultation.presentation.controller.conversationThreadController
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.consultationModule() {
    val chatService: ConsultationChatService by inject()
    val appointmentRepository: AppointmentRepository by inject()
    val joinCallUseCase: JoinThreadCallUseCase by inject()
    val endCallUseCase: EndThreadCallUseCase by inject()
    val meetingWebhookUseCase: HandleMeetingEndedWebhookUseCase by inject()
    val listThreadsUseCase: ListConversationThreadsUseCase by inject()
    val getMergedHistoryUseCase: GetMergedConsultationHistoryUseCase by inject()
    val hideConversationThreadUseCase: HideConversationThreadUseCase by inject()
    val socketRegistry: ConsultationSocketRegistry by inject()
    routing {
        consultationChatController(chatService, appointmentRepository, joinCallUseCase, endCallUseCase, meetingWebhookUseCase, socketRegistry)
        conversationThreadController(listThreadsUseCase, getMergedHistoryUseCase, hideConversationThreadUseCase, socketRegistry)
    }
}
