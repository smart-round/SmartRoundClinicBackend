package ke.co.smartroundclinic.payments.koin

import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.repository.IntaSendRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PaymentRepositoryImpl
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.domain.usecase.GetAllPaymentsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByAppointmentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByIdUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByDoctorUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByPatientUseCase
import ke.co.smartroundclinic.payments.domain.usecase.InitiatePaymentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.UpdatePaymentStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.payment.HandleIntaSendWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreateAppointmentPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreatePaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.GetPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.ListPaymentLinksUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.UpdatePaymentLinkUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val paymentsKoinModule = module {
    single<PaymentRepository> { PaymentRepositoryImpl(get(named("paymentsDb"))) }
    single<IntaSendRepository> { IntaSendRepositoryImpl(get(), AppConfig.intaSend) }

    // General payment use cases
    single { InitiatePaymentUseCase(get()) }
    single { GetPaymentByIdUseCase(get()) }
    single { GetPaymentByAppointmentUseCase(get()) }
    single { GetPaymentsByPatientUseCase(get()) }
    single { GetPaymentsByDoctorUseCase(get()) }
    single { GetAllPaymentsUseCase(get()) }
    single { UpdatePaymentStatusUseCase(get()) }
    single { PaymentService(get(), get(), get(), get(), get(), get(), get()) }

    // IntaSend payment-link use cases
    single { AppointmentInfoLookup(get(named("schedulingDb")), get(named("authDb")), get(named("adminDb"))) }
    single { CreatePaymentLinkUseCase(get(), AppConfig.intaSend) }
    single { ListPaymentLinksUseCase(get()) }
    single { GetPaymentLinkUseCase(get()) }
    single { UpdatePaymentLinkUseCase(get(), AppConfig.intaSend) }
    single { CreateAppointmentPaymentLinkUseCase(get(), get(), AppConfig.intaSend, get()) }
    single { HandleIntaSendWebhookUseCase(get()) }
    single { IntaSendService(get(), get(), get(), get(), get(), get()) }
}
