package ke.co.smartroundclinic.doctorchat

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatService
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatSocketRegistry
import ke.co.smartroundclinic.doctorchat.domain.usecase.GetDoctorChatHistoryUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.GetMyDoctorChatThreadsUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.InitiateDoctorChatUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.CancelDoctorCallInviteUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.DeclineDoctorCallInviteUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.EndDoctorCallUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.InviteToDoctorCallUseCase
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.JoinDoctorCallUseCase
import ke.co.smartroundclinic.doctorchat.presentation.controller.doctorChatController
import org.koin.ktor.ext.inject

fun Application.doctorChatModule() {
    val initiateUseCase: InitiateDoctorChatUseCase by inject()
    val listThreadsUseCase: GetMyDoctorChatThreadsUseCase by inject()
    val getHistoryUseCase: GetDoctorChatHistoryUseCase by inject()
    val chatService: DoctorChatService by inject()
    val threadRepository: DoctorChatThreadRepository by inject()
    val socketRegistry: DoctorChatSocketRegistry by inject()
    val joinCallUseCase: JoinDoctorCallUseCase by inject()
    val inviteToCallUseCase: InviteToDoctorCallUseCase by inject()
    val declineCallInviteUseCase: DeclineDoctorCallInviteUseCase by inject()
    val cancelCallInviteUseCase: CancelDoctorCallInviteUseCase by inject()
    val endCallUseCase: EndDoctorCallUseCase by inject()

    routing {
        doctorChatController(
            initiateUseCase = initiateUseCase,
            listThreadsUseCase = listThreadsUseCase,
            getHistoryUseCase = getHistoryUseCase,
            chatService = chatService,
            threadRepository = threadRepository,
            socketRegistry = socketRegistry,
            joinCallUseCase = joinCallUseCase,
            inviteToCallUseCase = inviteToCallUseCase,
            declineCallInviteUseCase = declineCallInviteUseCase,
            cancelCallInviteUseCase = cancelCallInviteUseCase,
            endCallUseCase = endCallUseCase,
        )
    }
}
