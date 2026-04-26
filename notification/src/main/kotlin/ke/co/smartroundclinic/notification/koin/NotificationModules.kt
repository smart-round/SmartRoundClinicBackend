package ke.co.smartroundclinic.notification.koin

import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.data.repository.EmailRepositoryImpl
import ke.co.smartroundclinic.notification.data.repository.NotificationRepositoryImpl
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import ke.co.smartroundclinic.notification.domain.repository.NotificationRepository
import ke.co.smartroundclinic.notification.domain.service.NotificationService
import ke.co.smartroundclinic.notification.domain.usecase.CreateNotificationUseCase
import ke.co.smartroundclinic.notification.domain.usecase.DeleteNotificationUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetAllNotificationsUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetMyNotificationsUseCase
import ke.co.smartroundclinic.notification.domain.usecase.GetNotificationByIdUseCase
import ke.co.smartroundclinic.notification.domain.usecase.MarkNotificationAsReadUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule = module {
    // ── Email (existing) ──────────────────────────────────────────────────────
    single<EmailRepository> { EmailRepositoryImpl(get(), get()) }
    single { EmailConfig() }
    single { AppConfig.resend }

    // ── In-app / push notifications ───────────────────────────────────────────
    single { NotificationRepositoryImpl(get(named("notificationDb"))) } bind NotificationSender::class
    single<NotificationRepository> { get<NotificationRepositoryImpl>() }

    single { CreateNotificationUseCase(get()) }
    single { GetNotificationByIdUseCase(get()) }
    single { GetMyNotificationsUseCase(get()) }
    single { GetAllNotificationsUseCase(get()) }
    single { MarkNotificationAsReadUseCase(get()) }
    single { DeleteNotificationUseCase(get()) }
    single { NotificationService(get(), get(), get(), get(), get(), get()) }
}
