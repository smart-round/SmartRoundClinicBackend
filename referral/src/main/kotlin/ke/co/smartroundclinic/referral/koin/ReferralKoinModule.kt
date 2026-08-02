package ke.co.smartroundclinic.referral.koin

import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.ReferralDisplayResolver
import ke.co.smartroundclinic.common.ReferralLinker
import ke.co.smartroundclinic.common.VerifiedDoctorResolver
import ke.co.smartroundclinic.referral.data.lookup.DoctorDisplayLookup
import ke.co.smartroundclinic.referral.data.repository.ReferralRepositoryImpl
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.domain.service.ReferralLinkerImpl
import ke.co.smartroundclinic.referral.domain.service.ReferralService
import ke.co.smartroundclinic.referral.domain.usecase.AcceptReferralUseCase
import ke.co.smartroundclinic.referral.domain.usecase.CreateReferralUseCase
import ke.co.smartroundclinic.referral.domain.usecase.DeclineReferralUseCase
import ke.co.smartroundclinic.referral.domain.usecase.GetMyReferralsUseCase
import ke.co.smartroundclinic.referral.domain.usecase.GetPendingReferralsUseCase
import ke.co.smartroundclinic.referral.domain.usecase.GetReferralHistoryUseCase
import ke.co.smartroundclinic.referral.domain.usecase.ReferralEligibilityUseCase
import ke.co.smartroundclinic.referral.domain.usecase.admin.GetAdminReferralStatsUseCase
import ke.co.smartroundclinic.referral.domain.usecase.admin.GetAdminReferralsUseCase
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val referralKoinModule = module {
    single<ReferralRepositoryImpl> { ReferralRepositoryImpl(get(named("referralDb"))) }
    single<ReferralRepository> { get<ReferralRepositoryImpl>() }
    single<ReferralDisplayResolver> { get<ReferralRepositoryImpl>() }
    single<ReferralLinker> { ReferralLinkerImpl(get()) }
    single { DoctorDisplayLookup(get(named("authDb"))) }

    single { ReferralEligibilityUseCase(get<AppointmentRepository>()) }
    single { CreateReferralUseCase(get(), get(), get(), get(), getOrNull<VerifiedDoctorResolver>(), getOrNull<NotificationSender>()) }
    single { AcceptReferralUseCase(get(), getOrNull<NotificationSender>()) }
    single { DeclineReferralUseCase(get(), getOrNull<NotificationSender>()) }
    single { GetMyReferralsUseCase(get(), get(), getOrNull<PatientNameResolver>()) }
    single { GetPendingReferralsUseCase(get(), get(), get()) }
    single { GetReferralHistoryUseCase(get(), get(), get()) }
    single { GetAdminReferralsUseCase(get(), get(), getOrNull<PatientNameResolver>()) }
    single { GetAdminReferralStatsUseCase(get()) }

    single { ReferralService(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
