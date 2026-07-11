package ke.co.smartroundclinic.doctor.koin

import ke.co.smartroundclinic.doctor.data.repository.DoctorProfileLookup
import ke.co.smartroundclinic.doctor.data.repository.RecommendationRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.DoctorRatingRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.CertificationRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.ComplianceCorrectionRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.ComplianceRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.PaymentDetailsRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.PractitionerLicenceRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.PractitionerProfileRepositoryImpl
import ke.co.smartroundclinic.common.DoctorSpecialitiesResolver
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.doctor.data.repository.SpecializationRepositoryImpl
import ke.co.smartroundclinic.doctor.domain.repository.RecommendationRepository
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository
import ke.co.smartroundclinic.doctor.domain.repository.CertificationRepository
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceCorrectionRepository
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import ke.co.smartroundclinic.doctor.domain.service.RecommendationService
import ke.co.smartroundclinic.doctor.domain.service.DoctorRatingService
import ke.co.smartroundclinic.doctor.domain.service.CertificationService
import ke.co.smartroundclinic.doctor.domain.service.ComplianceService
import ke.co.smartroundclinic.doctor.domain.service.PaymentDetailsService
import ke.co.smartroundclinic.doctor.domain.service.PractitionerLicenceService
import ke.co.smartroundclinic.doctor.domain.service.PractitionerProfileService
import ke.co.smartroundclinic.doctor.domain.service.SpecializationService
import ke.co.smartroundclinic.doctor.domain.usecase.recommendation.GetRecommendedDoctorsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.DeleteRatingUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.GetDoctorRatingsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.GetRatingByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.SubmitRatingUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.UpdateRatingUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.AddCertificationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.DeleteCertificationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.GetCertificationByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.GetMyCertificationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.UpdateCertificationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.ApproveComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.ConfirmComplianceCorrectionUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetAllComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetComplianceByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetComplianceCorrectionHistoryUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetLatestComplianceCorrectionsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetMyComplianceStatusUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.RejectComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.SubmitComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.AddLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.DeleteLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.GetAllLicencesUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.GetLicenceByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.GetMyLicencesUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.UpdateLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.CreatePractitionerProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetMyProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetPractitionerProfileByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetPractitionerProfilesUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetPractitionerProfileWithSpecializationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.UpdatePractitionerProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.AddPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.DeletePaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetAllPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetPaymentDetailsByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.UpdatePaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.AddSpecializationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.GetMySpecializationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.GetMySpecializationsWithDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.RemoveSpecializationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.UpdateSpecializationUseCase
import ke.co.smartroundclinic.doctor.data.repository.LocalBankRepositoryImpl
import ke.co.smartroundclinic.doctor.domain.repository.LocalBankRepository
import ke.co.smartroundclinic.doctor.domain.service.LocalBankService
import ke.co.smartroundclinic.doctor.domain.usecase.bank.FindLocalBankByCodeUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.FindLocalBanksByBranchCodeUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.GetAllLocalBanksUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.SearchLocalBanksByNameUseCase
import ke.co.smartroundclinic.auth.domain.usecase.NotifyNewDoctorSignUpUseCase
import ke.co.smartroundclinic.common.DoctorOnboardingHandler
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.ComplianceCheckUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.ToggleMonetizationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.signup.DoctorSignUpUseCase
import ke.co.smartroundclinic.infra.AppConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

val doctorModule = module {
    /**
     * Repositories
     * */
    single<PractitionerProfileRepository> { PractitionerProfileRepositoryImpl(get(named("doctorDb")), get(named("adminDb"))) }
    single<CertificationRepository> { CertificationRepositoryImpl(get(named("doctorDb"))) }
    single<ComplianceRepository> { ComplianceRepositoryImpl(get(named("doctorDb"))) }
    single<ComplianceCorrectionRepository> { ComplianceCorrectionRepositoryImpl(get(named("doctorDb"))) }
    single { DoctorProfileLookup(get(named("doctorDb")), get(named("authDb")), get(named("adminDb"))) }
    single<PractitionerLicenceRepository> { PractitionerLicenceRepositoryImpl(get(named("doctorDb"))) }
    single<SpecializationRepositoryImpl> { SpecializationRepositoryImpl(get(named("doctorDb")), get(named("adminDb"))) }
    single<SpecializationRepository> { get<SpecializationRepositoryImpl>() }
    single<DoctorSpecialitiesResolver> { get<SpecializationRepositoryImpl>() }
    single<PaymentDetailsRepository> { PaymentDetailsRepositoryImpl(get(named("doctorDb"))) }
    single<DoctorRatingRepository> { DoctorRatingRepositoryImpl(get(named("doctorDb")), get(named("schedulingDb"))) }
    single<RecommendationRepository> { RecommendationRepositoryImpl(get(named("doctorDb")), get(named("adminDb")), get(named("schedulingDb")), get(named("authDb"))) }

    /**
     * Profile
     * */
    single { CreatePractitionerProfileUseCase(get()) }
    single { UpdatePractitionerProfileUseCase(get()) }
    single { GetMyProfileUseCase(get()) }
    single { GetPractitionerProfileByIdUseCase(get()) }
    single { GetPractitionerProfilesUseCase(get()) }
    single { GetPractitionerProfileWithSpecializationsUseCase(get()) }

    /**
     * Certifications
     * */
    single { AddCertificationUseCase(get(), get()) }
    single { DeleteCertificationUseCase(get(), get()) }
    single { GetCertificationByIdUseCase(get(), get()) }
    single { GetMyCertificationsUseCase(get(), get()) }
    single { UpdateCertificationUseCase(get()) }

    /**
     * Licence
     * */
    single { AddLicenceUseCase(get(), get()) }
    single { DeleteLicenceUseCase(get(), get()) }
    single { GetAllLicencesUseCase(get(), get()) }
    single { GetLicenceByIdUseCase(get(), get()) }
    single { GetMyLicencesUseCase(get(), get()) }
    single { UpdateLicenceUseCase(get()) }

    /**
     * Compliance
     * */
    single { ApproveComplianceUseCase(get(), get(), get(), get(), getOrNull()) }
    single { GetAllComplianceUseCase(get(), get()) }
    single { GetComplianceByIdUseCase(get(), get()) }
    single { GetMyComplianceStatusUseCase(get(), get()) }
    single { RejectComplianceUseCase(get(), get(), get(), get(), getOrNull()) }
    single { SubmitComplianceUseCase(get(),get(),get(),get()) }
    single { ComplianceCheckUseCase(get(), get()) }
    single { ToggleMonetizationUseCase(get(), get()) }
    single { ConfirmComplianceCorrectionUseCase(get(), get()) }
    single { GetLatestComplianceCorrectionsUseCase(get()) }
    single { GetComplianceCorrectionHistoryUseCase(get()) }

    /**
     * Specialization
     * */
    single { AddSpecializationUseCase(get()) }
    single { GetMySpecializationsUseCase(get()) }
    single { GetMySpecializationsWithDetailsUseCase(get()) }
    single { RemoveSpecializationUseCase(get()) }
    single { UpdateSpecializationUseCase(get()) }

    /**
     * Payment Details
     * */
    single { AddPaymentDetailsUseCase(get()) }
    single { GetPaymentDetailsUseCase(get()) }
    single { UpdatePaymentDetailsUseCase(get()) }
    single { DeletePaymentDetailsUseCase(get()) }
    single { GetAllPaymentDetailsUseCase(get()) }
    single { GetPaymentDetailsByIdUseCase(get()) }

    /**
     * Services
     * */
    single {
        PractitionerProfileService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        CertificationService(
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        PractitionerLicenceService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        ComplianceService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    single {
        SpecializationService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    single {
        PaymentDetailsService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    /**
     * Doctor Sign Up
     * */
    single { DoctorSignUpUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), getOrNull<NotifyNewDoctorSignUpUseCase>()) }
    single<DoctorOnboardingHandler> { get<DoctorSignUpUseCase>() }

    /**
     * Recommendations
     * */
    single { GetRecommendedDoctorsUseCase(get(), get()) }
    single { RecommendationService(get()) }

    /**
     * Ratings
     * */
    single { SubmitRatingUseCase(get()) }
    single { UpdateRatingUseCase(get()) }
    single { DeleteRatingUseCase(get()) }
    single { GetDoctorRatingsUseCase(get(), getOrNull<PatientNameResolver>(), getOrNull<UserProfilePictureResolver>()) }
    single { GetRatingByIdUseCase(get()) }
    single { DoctorRatingService(get(), get(), get(), get(), get()) }

    /**
     * Local Banks
     * */
    single<LocalBankRepository> { LocalBankRepositoryImpl(get(named("doctorDb")), AppConfig.staticAssets.kenyanBanksJsonUrl) }
    single { GetAllLocalBanksUseCase(get()) }
    single { SearchLocalBanksByNameUseCase(get()) }
    single { FindLocalBankByCodeUseCase(get()) }
    single { FindLocalBanksByBranchCodeUseCase(get()) }
    single { LocalBankService(get(), get(), get(), get()) }
}

