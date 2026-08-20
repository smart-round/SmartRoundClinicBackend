package ke.co.smartroundclinic.notification.koin

import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.config.FcmConfig
import ke.co.smartroundclinic.notification.data.repository.EmailRepositoryImpl
import ke.co.smartroundclinic.notification.data.repository.NotificationRepositoryImpl
import ke.co.smartroundclinic.notification.data.repository.PushNotificationRepositoryImpl
import ke.co.smartroundclinic.notification.data.repository.UserDeviceTokenRepositoryImpl
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import ke.co.smartroundclinic.notification.domain.repository.NotificationRepository
import ke.co.smartroundclinic.notification.domain.repository.PushNotificationRepository
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import ke.co.smartroundclinic.notification.domain.service.NotificationService
import ke.co.smartroundclinic.notification.domain.usecase.CreateNotificationUseCase
import ke.co.smartroundclinic.notification.domain.usecase.DeleteNotificationUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetAllNotificationsUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetMyNotificationsUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetNotificationByIdUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetPushNotificationLogsUseCase
import ke.co.smartroundclinic.notification.domain.usecase.MarkNotificationAsReadUseCase
import ke.co.smartroundclinic.notification.domain.usecase.RegisterDeviceTokenUseCase
import ke.co.smartroundclinic.notification.domain.usecase.SendPushNotificationUseCase
import ke.co.smartroundclinic.notification.domain.usecase.StaleDeviceTokenCleanupTask
import ke.co.smartroundclinic.notification.domain.usecase.UnregisterDeviceTokenUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule = module {
    // ── Email ─────────────────────────────────────────────────────────────────
    single<EmailRepository> { EmailRepositoryImpl(get(), get()) }
    single { EmailConfig() }
    single { AppConfig.resend }

    // ── FCM ───────────────────────────────────────────────────────────────────
    single { FcmConfig() }

    single<UserDeviceTokenRepository> { UserDeviceTokenRepositoryImpl(get(named("notificationDb"))) }
    single<PushNotificationRepository> {
        PushNotificationRepositoryImpl(get(named("notificationDb")), get<FcmConfig>().messaging, get())
    }

    // ── In-app notifications (also dispatches FCM on PUSH_NOTIFICATION channel) ──
    single { NotificationRepositoryImpl(get(named("notificationDb")), get(), get(), get()) } bind NotificationSender::class
    single<NotificationRepository> { get<NotificationRepositoryImpl>() }

    // ── Use cases ──────────────────────────────────────────────────────────────
    single { CreateNotificationUseCase(get()) }
    single { GetNotificationByIdUseCase(get()) }
    single { GetMyNotificationsUseCase(get()) }
    single { GetAllNotificationsUseCase(get()) }
    single { MarkNotificationAsReadUseCase(get()) }
    single { DeleteNotificationUseCase(get()) }

    single { RegisterDeviceTokenUseCase(get()) }
    single { UnregisterDeviceTokenUseCase(get()) }
    single { SendPushNotificationUseCase(get(), get()) }
    single { GetPushNotificationLogsUseCase(get()) }
    single { StaleDeviceTokenCleanupTask(get()) }

    single {
        NotificationService(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(),
        )
    }
}
