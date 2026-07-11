package ke.co.smartroundclinic.consultation.koin

import ke.co.smartroundclinic.consultation.data.repository.ConsultationHiddenThreadRepositoryImpl
import ke.co.smartroundclinic.consultation.data.repository.ConsultationMessageRepositoryImpl
import ke.co.smartroundclinic.consultation.data.repository.ConsultationThreadRepositoryImpl
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadRepository
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.domain.usecase.call.CancelCallInviteUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.DeclineCallInviteUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndThreadCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.HandleMeetingEndedWebhookUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.InviteToCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinThreadCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.StaleCallCleanupTask
import ke.co.smartroundclinic.consultation.domain.usecase.chat.GetMergedConsultationHistoryUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.HideConversationThreadUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.ListConversationThreadsUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.chat.NotifyOfflineConsultationParticipantUseCase
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
    single<ConsultationThreadRepository> {
        ConsultationThreadRepositoryImpl(get(named("consultationDb")))
    }
    single<ConsultationMessageRepository> {
        ConsultationMessageRepositoryImpl(get(named("consultationDb")), get(named("authDb")), get<StorageRepository>())
    }
    single<ConsultationHiddenThreadRepository> {
        ConsultationHiddenThreadRepositoryImpl(get(named("consultationDb")))
    }

    /**
     * Chat use cases
     */
    single { GetMergedConsultationHistoryUseCase(get(), get<StorageRepository>()) }
    single { ListConversationThreadsUseCase(get(), get(), get(), get<RedisRepository>()) }
    single { NotifyOfflineConsultationParticipantUseCase(get<RedisRepository>(), getOrNull<NotificationSender>()) }
    single { HideConversationThreadUseCase(get()) }

    /**
     * Call (Cloudflare RealtimeKit) use cases
     */
    single { JoinThreadCallUseCase(get(), get(), get<RealtimeKitClient>(), getOrNull(), get<RedisRepository>(), get()) }
    single { EndThreadCallUseCase(get(), get<RealtimeKitClient>(), getOrNull()) }
    single { HandleMeetingEndedWebhookUseCase(get(), get<RealtimeKitClient>(), getOrNull()) }
    single { StaleCallCleanupTask(get<RealtimeKitClient>(), get()) }

    /**
     * Real-time infra
     */
    single { ConsultationSocketRegistry() }

    /**
     * Incoming-call ringing (invite/answer/decline/cancel)
     */
    single { InviteToCallUseCase(get(), get(), get<RedisRepository>(), get(), getOrNull()) }
    single { DeclineCallInviteUseCase(get<RedisRepository>(), get(), getOrNull()) }
    single { CancelCallInviteUseCase(get<RedisRepository>(), get(), getOrNull()) }

    /**
     * Services
     */
    single { ConsultationChatService(get(), get<StorageRepository>(), get(), get(), get<RedisRepository>()) }
}
