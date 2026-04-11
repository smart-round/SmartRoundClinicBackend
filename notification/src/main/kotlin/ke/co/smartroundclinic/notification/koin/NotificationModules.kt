package ke.co.smartroundclinic.notification.koin

import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.data.repository.EmailRepositoryImpl
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import org.koin.dsl.module

val notificationModule =
    module {
        single<EmailRepository> { EmailRepositoryImpl(get(), get()) }
        single { EmailConfig() }
        single { AppConfig.resend }
    }
