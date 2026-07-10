package ke.co.smartroundclinic.consultation.koin

import ke.co.smartroundclinic.consultation.data.repository.ConsultationHiddenThreadRepositoryImpl
import ke.co.smartroundclinic.consultation.data.repository.ConsultationMessageRepositoryImpl
import ke.co.smartroundclinic.consultation.data.repository.ConsultationSessionRepositoryImpl
import ke.co.smartroundclinic.consultation.data.repository.ConsultationThreadReadStateRepositoryImpl
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadReadStateRepository
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSessionService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.HandleMeetingEndedWebhookUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinConsultationCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.StaleCallCleanupTask
import ke.co.smartroundclinic.consultation.domain.usecase.chat.BackfillMessageThreadFieldsUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetMergedConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.HideConversationThreadUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.ListConversationThreadsUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.MarkThreadReadUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.NotifyOfflineConsultationParticipantUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.session.EndConsultationUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.session.GetConsultationUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.session.StartConsultationUseCase
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.RedisRepository
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
        ConsultationMessageRepositoryImpl(get(named("consultationDb")), get(named("authDb")), get<StorageRepository>())
    }
    single<ConsultationThreadReadStateRepository> {
        ConsultationThreadReadStateRepositoryImpl(get(named("consultationDb")))
    }
    single<ConsultationHiddenThreadRepository> {
        ConsultationHiddenThreadRepositoryImpl(get(named("consultationDb")))
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
    single { GetConsultationHistoryUseCase(get(), get<StorageRepository>()) }
    single { GetMergedConsultationHistoryUseCase(get(), get<StorageRepository>(), get()) }
    single { ListConversationThreadsUseCase(get(), get(), get(), get<RedisRepository>()) }
    single { NotifyOfflineConsultationParticipantUseCase(get<RedisRepository>(), getOrNull<NotificationSender>()) }
    single { BackfillMessageThreadFieldsUseCase(get(), get()) }
    single { MarkThreadReadUseCase(get()) }
    single { HideConversationThreadUseCase(get()) }

    /**
     * Call (Cloudflare RealtimeKit) use cases
     */
    single { JoinConsultationCallUseCase(get(), get(), get<RealtimeKitClient>(), getOrNull()) }
    single { EndCallUseCase(get(), get<RealtimeKitClient>(), getOrNull()) }
    single { HandleMeetingEndedWebhookUseCase(get(), get<RealtimeKitClient>(), getOrNull()) }
    single { StaleCallCleanupTask(get<RealtimeKitClient>(), get()) }

    /**
     * Real-time infra
     */
    single { ConsultationSocketRegistry() }

    /**
     * Services
     */
    single { ConsultationSessionService(get(), get(), get(), get()) }
    single { ConsultationChatService(get(), get<StorageRepository>(), get(), get(), get(), get<RedisRepository>(), get()) }
}
