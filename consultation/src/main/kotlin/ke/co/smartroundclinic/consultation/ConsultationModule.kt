package ke.co.smartroundclinic.consultation

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSessionService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.HandleMeetingEndedWebhookUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinConsultationCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetMergedConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.HideConversationThreadUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.ListConversationThreadsUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.MarkThreadReadUseCase
import ke.co.smartroundclinic.consultation.presentation.controller.consultationChatController
import ke.co.smartroundclinic.consultation.presentation.controller.consultationSessionController
import ke.co.smartroundclinic.consultation.presentation.controller.conversationThreadController
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.consultationModule() {
    val sessionService: ConsultationSessionService by inject()
    val chatService: ConsultationChatService by inject()
    val joinCallUseCase: JoinConsultationCallUseCase by inject()
    val endCallUseCase: EndCallUseCase by inject()
    val meetingWebhookUseCase: HandleMeetingEndedWebhookUseCase by inject()
    val listThreadsUseCase: ListConversationThreadsUseCase by inject()
    val getMergedHistoryUseCase: GetMergedConsultationHistoryUseCase by inject()
    val markThreadReadUseCase: MarkThreadReadUseCase by inject()
    val hideConversationThreadUseCase: HideConversationThreadUseCase by inject()
    val socketRegistry: ConsultationSocketRegistry by inject()
    routing {
        consultationSessionController(sessionService)
        consultationChatController(chatService, sessionService, joinCallUseCase, endCallUseCase, meetingWebhookUseCase, socketRegistry)
        conversationThreadController(listThreadsUseCase, getMergedHistoryUseCase, markThreadReadUseCase, hideConversationThreadUseCase, socketRegistry)
    }
}
