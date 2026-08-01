package ke.co.smartroundclinic.doctorchat.koin

import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.VerifiedDoctorResolver
import ke.co.smartroundclinic.doctorchat.data.repository.DoctorChatMessageRepositoryImpl
import ke.co.smartroundclinic.doctorchat.data.repository.DoctorChatThreadRepositoryImpl
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
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
import ke.co.smartroundclinic.doctorchat.domain.usecase.call.StaleDoctorCallCleanupTask
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import ke.co.smartroundclinic.infra.storage.StorageRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val doctorChatKoinModule = module {

    /**
     * Repositories
     */
    single<DoctorChatThreadRepository> { DoctorChatThreadRepositoryImpl(get(named("doctorChatDb"))) }
    single<DoctorChatMessageRepository> { DoctorChatMessageRepositoryImpl(get(named("doctorChatDb")), get(named("authDb")), get<StorageRepository>()) }

    /**
     * Real-time infra
     */
    single { DoctorChatSocketRegistry() }
    single { DoctorChatService(get(), get<StorageRepository>(), get(), get<RedisRepository>(), getOrNull()) }

    /**
     * Chat use cases
     */
    single { InitiateDoctorChatUseCase(get(), get(), getOrNull<VerifiedDoctorResolver>()) }
    single { GetMyDoctorChatThreadsUseCase(get(), get(), get<RedisRepository>()) }
    single { GetDoctorChatHistoryUseCase(get(), get()) }

    /**
     * Call (Cloudflare RealtimeKit) use cases
     */
    single { JoinDoctorCallUseCase(get(), get(), get<RealtimeKitClient>(), getOrNull(), get<RedisRepository>(), get()) }
    single { InviteToDoctorCallUseCase(get(), get(), get<RedisRepository>(), get(), getOrNull()) }
    single { DeclineDoctorCallInviteUseCase(get<RedisRepository>(), get(), getOrNull()) }
    single { CancelDoctorCallInviteUseCase(get<RedisRepository>(), get(), getOrNull()) }
    single { EndDoctorCallUseCase(get(), get<RealtimeKitClient>(), getOrNull()) }
    single { StaleDoctorCallCleanupTask(get<RealtimeKitClient>(), get()) }
}
