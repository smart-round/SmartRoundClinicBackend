package ke.co.smartroundclinic.payments.koin

import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.lookup.DoctorPaymentDetailsLookup
import ke.co.smartroundclinic.payments.data.lookup.DoctorTierPriceLookup
import ke.co.smartroundclinic.payments.data.repository.IntaSendRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PaymentLogRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PaymentRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PlatformCommissionLogRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.WithdrawalRepositoryImpl
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentLogRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.PlatformCommissionLogRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.domain.service.AdminPaymentsService
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.domain.usecase.GetAllPaymentsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetDoctorPaymentSummaryUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByAppointmentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentByIdUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByDoctorUseCase
import ke.co.smartroundclinic.payments.domain.usecase.GetPaymentsByPatientUseCase
import ke.co.smartroundclinic.payments.domain.usecase.InitiatePaymentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.UpdatePaymentStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.payment.HandleIntaSendWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreateAppointmentPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreatePaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.CreatePreBookingPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.GetPaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.ListPaymentLinksUseCase
import ke.co.smartroundclinic.payments.domain.usecase.paymentlink.UpdatePaymentLinkUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetAllWithdrawalsAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetCommissionLogsAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetCommissionTimeSummaryUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetEarningsChartUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetDoctorPaymentBreakdownUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetPlatformOverviewUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.CheckWithdrawalStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.GetWithdrawalBalanceUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.GetWithdrawalHistoryUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.HandleWithdrawalWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.WithdrawalUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val paymentsKoinModule = module {
    // Repositories
    single<PaymentRepository> { PaymentRepositoryImpl(get(named("paymentsDb"))) }
    single<PaymentLogRepository> { PaymentLogRepositoryImpl(get(named("paymentsDb"))) }
    single<WithdrawalRepository> { WithdrawalRepositoryImpl(get(named("paymentsDb"))) }
    single<PlatformCommissionLogRepository> { PlatformCommissionLogRepositoryImpl(get(named("paymentsDb"))) }
    single<IntaSendRepository> { IntaSendRepositoryImpl(get(), AppConfig.intaSend) }

    // General payment use cases
    single { InitiatePaymentUseCase(get(), get()) }
    single { GetPaymentByIdUseCase(get()) }
    single { GetPaymentByAppointmentUseCase(get()) }
    single { GetPaymentsByPatientUseCase(get()) }
    single { GetPaymentsByDoctorUseCase(get()) }
    single { GetAllPaymentsUseCase(get()) }
    single { UpdatePaymentStatusUseCase(get()) }
    single { GetDoctorPaymentSummaryUseCase(get(), get()) }
    single { PaymentService(get(), get(), get(), get(), get(), get(), get(), get()) }

    // IntaSend payment-link use cases
    single { AppointmentInfoLookup(get(named("schedulingDb")), get(named("authDb")), get(named("adminDb"))) }
    single { DoctorPaymentDetailsLookup(get(named("doctorDb"))) }
    single { DoctorTierPriceLookup(get(named("adminDb")), get(named("doctorDb"))) }
    single { CreatePaymentLinkUseCase(get(), AppConfig.intaSend) }
    single { ListPaymentLinksUseCase(get()) }
    single { GetPaymentLinkUseCase(get()) }
    single { UpdatePaymentLinkUseCase(get(), AppConfig.intaSend) }
    single { CreateAppointmentPaymentLinkUseCase(get(), get(), AppConfig.intaSend, get()) }
    single { CreatePreBookingPaymentLinkUseCase(get(), get(), AppConfig.intaSend, get(), get()) }
    single { HandleIntaSendWebhookUseCase(get(), get()) }

    // Withdrawal use cases
    single { WithdrawalUseCase(get(), get(), get(), get(), get(), AppConfig.intaSend) }
    single { GetWithdrawalBalanceUseCase(get(), get()) }
    single { GetWithdrawalHistoryUseCase(get()) }
    single { CheckWithdrawalStatusUseCase(get()) }
    single { HandleWithdrawalWebhookUseCase(get()) }

    single { IntaSendService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    // Admin use cases + service
    single { GetPlatformOverviewUseCase(get(), get(), get()) }
    single { GetDoctorPaymentBreakdownUseCase(get(), get()) }
    single { GetAllWithdrawalsAdminUseCase(get()) }
    single { GetCommissionLogsAdminUseCase(get()) }
    single { GetCommissionTimeSummaryUseCase(get()) }
    single { GetEarningsChartUseCase(get()) }
    single { AdminPaymentsService(get(), get(), get(), get(), get(), get()) }
}
