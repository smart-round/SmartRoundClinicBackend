package ke.co.smartroundclinic.consultation.koin

import ke.co.smartroundclinic.consultation.data.repository.ConsultationMessageRepositoryImpl
import ke.co.smartroundclinic.consultation.data.repository.ConsultationSessionRepositoryImpl
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSessionService
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.HandleMeetingEndedWebhookUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinConsultationCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.session.EndConsultationUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.session.GetConsultationUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.session.StartConsultationUseCase
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import ke.co.smartroundclinic.infra.storage.StorageRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val consultationKoinModule = module {

    /**
     * Repositories
     */
    single<ConsultationSessionRepository> {
        ConsultationSessionRepositoryImpl(get(named("consultationDb")), get(named("schedulingDb")))
    }
    single<ConsultationMessageRepository> {
        ConsultationMessageRepositoryImpl(get(named("consultationDb")), get(named("authDb")))
    }

    /**
     * Session use cases
     */
    single { StartConsultationUseCase(get(), getOrNull()) }
    single { GetConsultationUseCase(get()) }
    single { EndConsultationUseCase(get(), getOrNull()) }

    /**
     * Chat use cases
     */
    single { GetConsultationHistoryUseCase(get()) }

    /**
     * Call (Cloudflare RealtimeKit) use cases
     */
    single { JoinConsultationCallUseCase(get(), get(), get<RealtimeKitClient>(), getOrNull()) }
    single { EndCallUseCase(get(), get<RealtimeKitClient>(), getOrNull()) }
    single { HandleMeetingEndedWebhookUseCase(get(), get<RealtimeKitClient>(), getOrNull()) }

    /**
     * Services
     */
    single { ConsultationSessionService(get(), get(), get(), get()) }
    single { ConsultationChatService(get(), get<StorageRepository>(), get()) }
}
