package ke.co.smartroundclinic.payments.koin

import ke.co.smartroundclinic.common.AppointmentEarningsCreditor
import ke.co.smartroundclinic.common.DoctorWalletProvisioner
import ke.co.smartroundclinic.common.RefundProcessor
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.payments.data.lookup.AppointmentInfoLookup
import ke.co.smartroundclinic.payments.data.lookup.DoctorPaymentDetailsLookup
import ke.co.smartroundclinic.payments.data.lookup.DoctorTierPriceLookup
import ke.co.smartroundclinic.payments.data.repository.IntaSendRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PaymentLogRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PaymentRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.PlatformCommissionLogRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.RefundRepositoryImpl
import ke.co.smartroundclinic.payments.data.repository.WithdrawalRepositoryImpl
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentLogRepository
import ke.co.smartroundclinic.payments.domain.repository.PaymentRepository
import ke.co.smartroundclinic.payments.domain.repository.PlatformCommissionLogRepository
import ke.co.smartroundclinic.payments.domain.repository.RefundRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.domain.service.AdminPaymentsService
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
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
import ke.co.smartroundclinic.payments.domain.usecase.earnings.CreditDoctorEarningsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.payment.HandleIntaSendWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.refund.HandleRefundWebhookUseCase
import ke.co.smartroundclinic.payments.domain.usecase.refund.ProcessRefundUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetAllWithdrawalsAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetCommissionLogsAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetCommissionTimeSummaryUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetEarningsChartUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetDoctorPaymentBreakdownUseCase
import ke.co.smartroundclinic.payments.domain.usecase.admin.GetPlatformOverviewUseCase
import ke.co.smartroundclinic.payments.domain.usecase.stkpush.GetStkPushPaymentStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.stkpush.InitiateStkPushAppointmentUseCase
import ke.co.smartroundclinic.payments.domain.usecase.stkpush.InitiateStkPushPreBookingUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetDoctorWalletBalanceAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetDoctorWalletStatementAdminUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetPlatformWalletBalanceUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetPlatformWalletStatementUseCase
import ke.co.smartroundclinic.payments.domain.usecase.wallet.GetWalletTransactionsUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.CheckWithdrawalStatusUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.GetWithdrawalBalanceUseCase
import ke.co.smartroundclinic.payments.domain.usecase.withdrawal.GetWithdrawalByIdUseCase
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
    single<RefundRepository> { RefundRepositoryImpl(get(named("paymentsDb"))) }
    single<IntaSendRepository> { IntaSendRepositoryImpl(get(), AppConfig.intaSend) }

    // Lookups shared with STK push / withdrawal / earnings-credit use cases
    single { AppointmentInfoLookup(get(named("schedulingDb")), get(named("authDb")), get(named("adminDb"))) }
    single { DoctorPaymentDetailsLookup(get(named("doctorDb"))) }
    single { DoctorTierPriceLookup(get(named("adminDb")), get(named("doctorDb"))) }

    // Doctor wallet resolution — bound as both the concrete class (internal :payments use) and
    // the cross-module DoctorWalletProvisioner interface (consumed by :doctor's AddPaymentDetailsUseCase)
    single { DoctorWalletResolver(get(), get(), AppConfig.intaSend) }
    single<DoctorWalletProvisioner> { get<DoctorWalletResolver>() }

    // General payment use cases
    single { InitiatePaymentUseCase(get(), get()) }
    single { GetPaymentByIdUseCase(get()) }
    single { GetPaymentByAppointmentUseCase(get()) }
    single { GetPaymentsByPatientUseCase(get()) }
    single { GetPaymentsByDoctorUseCase(get()) }
    single { GetAllPaymentsUseCase(get()) }
    single { UpdatePaymentStatusUseCase(get()) }
    single { GetDoctorPaymentSummaryUseCase(get(), get(), get(), get()) }
    single { PaymentService(get(), get(), get(), get(), get(), get(), get(), get()) }

    single { HandleIntaSendWebhookUseCase(get(), get()) }

    // STK push use cases
    single { InitiateStkPushAppointmentUseCase(get(), get(), AppConfig.intaSend, get()) }
    single { InitiateStkPushPreBookingUseCase(get(), get(), AppConfig.intaSend, get(), get()) }
    single { GetStkPushPaymentStatusUseCase(get()) }

    // Earnings-credit use case — triggered once, immediately, from CompleteAppointmentUseCase
    // when an appointment is marked COMPLETED. No background sweep; see CreditDoctorEarningsUseCase.
    single { CreditDoctorEarningsUseCase(get(), get(), get(), get(), AppConfig.intaSend) }
    single<AppointmentEarningsCreditor> { get<CreditDoctorEarningsUseCase>() }

    // Withdrawal use cases — disbursement is trigger-only (doctor's POST /withdraw request);
    // status updates come from the IntaSend webhook, no polling reconciliation task.
    single { WithdrawalUseCase(get(), get(), get(), get(), AppConfig.intaSend) }
    single { GetWithdrawalBalanceUseCase(get(), get(), get(), get()) }
    single { GetWithdrawalHistoryUseCase(get(), get()) }
    single { GetWithdrawalByIdUseCase(get(), get()) }
    single { CheckWithdrawalStatusUseCase(get()) }
    single { HandleWithdrawalWebhookUseCase(get()) }

    // Doctor wallet transaction ledger — live IntaSend proxy
    single { GetWalletTransactionsUseCase(get(), get()) }

    // Refund processing — triggered once, immediately, from CancelAppointmentUseCase right after
    // the refund record is created. No background poller; see ProcessRefundUseCase.
    single { ProcessRefundUseCase(get(), get(), get(), AppConfig.intaSend) }
    single<RefundProcessor> { get<ProcessRefundUseCase>() }
    single { HandleRefundWebhookUseCase(get()) }

    single { IntaSendService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    // Admin use cases + service
    single { GetPlatformOverviewUseCase(get(), get(), get()) }
    single { GetPlatformWalletBalanceUseCase(get()) }
    single { GetPlatformWalletStatementUseCase(get()) }
    single { GetDoctorWalletBalanceAdminUseCase(get(), get()) }
    single { GetDoctorWalletStatementAdminUseCase(get(), get()) }
    single { GetDoctorPaymentBreakdownUseCase(get(), get()) }
    single { GetAllWithdrawalsAdminUseCase(get()) }
    single { GetCommissionLogsAdminUseCase(get()) }
    single { GetCommissionTimeSummaryUseCase(get()) }
    single { GetEarningsChartUseCase(get()) }
    single { AdminPaymentsService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), AppConfig.intaSend) }
}
