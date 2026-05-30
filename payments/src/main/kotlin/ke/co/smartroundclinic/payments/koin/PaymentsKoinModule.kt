package ke.co.smartroundclinic.payments.koin

import ke.co.smartroundclinic.payments.data.repository.PaymentRepositoryImpl
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.domain.usecase.GetAllPaymentsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByAppointmentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByIdUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByDoctorUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByPatientUseCase
import ke.co.smartroundclinic.payments.domain.usecase.InitiatePaymentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.UpdatePaymentStatusUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val paymentsKoinModule = module {
    single<PaymentRepository> { PaymentRepositoryImpl(get(named("paymentsDb"))) }

    single { InitiatePaymentUseCase(get()) }
    single { GetPaymentByIdUseCase(get()) }
    single { GetPaymentByAppointmentUseCase(get()) }
    single { GetPaymentsByPatientUseCase(get()) }
    single { GetPaymentsByDoctorUseCase(get()) }
    single { GetAllPaymentsUseCase(get()) }
    single { UpdatePaymentStatusUseCase(get()) }
    single { PaymentService(get(), get(), get(), get(), get(), get(), get()) }
}
