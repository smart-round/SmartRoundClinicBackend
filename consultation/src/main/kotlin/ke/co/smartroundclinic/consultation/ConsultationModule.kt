package ke.co.smartroundclinic.consultation

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSessionService
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinConsultationCallUseCase
import ke.co.smartroundclinic.consultation.presentation.controller.consultationChatController
import ke.co.smartroundclinic.consultation.presentation.controller.consultationSessionController
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.consultationModule() {
    val sessionService: ConsultationSessionService by inject()
    val chatService: ConsultationChatService by inject()
    val joinCallUseCase: JoinConsultationCallUseCase by inject()
    val endCallUseCase: EndCallUseCase by inject()
    routing {
        consultationSessionController(sessionService)
        consultationChatController(chatService, sessionService, joinCallUseCase, endCallUseCase)
    }
}
