package ke.co.smartroundclinic.support.koin

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.support.data.repository.IssueCategoryRepositoryImpl
import ke.co.smartroundclinic.support.data.repository.SupportTicketChatRepositoryImpl
import ke.co.smartroundclinic.support.data.repository.TicketRepositoryImpl
import ke.co.smartroundclinic.support.domain.repository.IssueCategoryRepository
import ke.co.smartroundclinic.support.domain.repository.SupportTicketChatRepository
import ke.co.smartroundclinic.support.domain.repository.TicketRepository
import ke.co.smartroundclinic.support.domain.service.IssueCategoryService
import ke.co.smartroundclinic.support.domain.service.SupportTicketChatService
import ke.co.smartroundclinic.support.domain.service.TicketService
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.CreateIssueCategoryUseCase
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.DeleteIssueCategoryUseCase
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.GetAllIssueCategoriesUseCase
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.GetIssueCategoryByIdUseCase
import ke.co.smartroundclinic.support.domain.usecase.issueCategory.UpdateIssueCategoryUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.AssignTicketUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.CreateTicketUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.DeleteTicketUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.GetAllTicketsUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.GetTicketByIdUseCase
import ke.co.smartroundclinic.support.domain.usecase.ticket.UpdateTicketStatusUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val supportModule = module {
    single<IssueCategoryRepository> { IssueCategoryRepositoryImpl(get(named("supportDb"))) }
    single<TicketRepository> { TicketRepositoryImpl(get(named("supportDb")), get(named("authDb"))) }
    single<SupportTicketChatRepository> { SupportTicketChatRepositoryImpl(get(named("supportDb")), get(named("authDb"))) }
    single { SupportTicketChatService(get<SupportTicketChatRepository>(), get<StorageRepository>()) }

    single { CreateIssueCategoryUseCase(get()) }
    single { GetIssueCategoryByIdUseCase(get()) }
    single { GetAllIssueCategoriesUseCase(get()) }
    single { UpdateIssueCategoryUseCase(get()) }
    single { DeleteIssueCategoryUseCase(get()) }
    single { IssueCategoryService(get(), get(), get(), get(), get()) }

    single { CreateTicketUseCase(get()) }
    single { GetTicketByIdUseCase(get()) }
    single { GetAllTicketsUseCase(get()) }
    single { UpdateTicketStatusUseCase(get()) }
    single { AssignTicketUseCase(get()) }
    single { DeleteTicketUseCase(get()) }
    single { TicketService(get(), get(), get(), get(), get(), get()) }
}
