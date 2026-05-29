package ke.co.smartroundclinic.payments.koin

import ke.co.smartroundclinic.common.AccountResolver
import ke.co.smartroundclinic.common.SubAccountCreator
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.payments.data.repository.AccountResolverImpl
import ke.co.smartroundclinic.payments.data.repository.PaymentRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PaystackRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PaystackSubAccountCreatorImpl
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.domain.service.SubAccountService
import ke.co.smartroundclinic.payments.domain.service.VerificationService
import ke.co.smartroundclinic.payments.domain.usecase.GetAllPaymentsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByAppointmentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByIdUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByDoctorUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByPatientUseCase
import ke.co.smartroundclinic.payments.domain.usecase.InitiatePaymentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.UpdatePaymentStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.CreateMySubAccountUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.GetMySubAccountUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.GetSubAccountUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.ListSubAccountsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.subaccount.UpdateMySubAccountUseCase
import ke.co.smartroundclinic.payments.domain.usecase.verification.ResolveCardBinUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val paymentsKoinModule = module {
    single<PaymentRepository> { PaymentRepositoryImpl(get(named("paymentsDb"))) }
    single<PaystackRepository> { PaystackRepositoryImpl(get(), AppConfig.paystack) }
    single<SubAccountCreator> { PaystackSubAccountCreatorImpl(get(), get(named("adminDb")), get(named("doctorDb"))) }
    single<AccountResolver> { AccountResolverImpl(get()) }

    // Payment use cases
    single { InitiatePaymentUseCase(get()) }
    single { GetPaymentByIdUseCase(get()) }
    single { GetPaymentByAppointmentUseCase(get()) }
    single { GetPaymentsByPatientUseCase(get()) }
    single { GetPaymentsByDoctorUseCase(get()) }
    single { GetAllPaymentsUseCase(get()) }
    single { UpdatePaymentStatusUseCase(get()) }
    single { PaymentService(get(), get(), get(), get(), get(), get(), get()) }

    // Subaccount use cases
    single { CreateMySubAccountUseCase(get(), get(named("doctorDb")), get(named("adminDb"))) }
    single { GetMySubAccountUseCase(get(), get(named("doctorDb"))) }
    single { UpdateMySubAccountUseCase(get(), get(named("doctorDb"))) }
    single { ListSubAccountsUseCase(get()) }
    single { GetSubAccountUseCase(get()) }
    single { SubAccountService(get(), get(), get(), get(), get()) }

    // Verification use cases
    single { ResolveCardBinUseCase(get()) }
    single { VerificationService(get()) }
}
