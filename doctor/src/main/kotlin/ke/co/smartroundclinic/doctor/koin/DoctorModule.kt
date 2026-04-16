package ke.co.smartroundclinic.doctor.koin

import ke.co.smartroundclinic.doctor.data.repository.CertificationRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.ComplianceRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.PaymentDetailsRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.PractitionerLicenceRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.PractitionerProfileRepositoryImpl
import ke.co.smartroundclinic.doctor.data.repository.SpecializationRepositoryImpl
import ke.co.smartroundclinic.doctor.domain.repository.CertificationRepository
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import ke.co.smartroundclinic.doctor.domain.service.CertificationService
import ke.co.smartroundclinic.doctor.domain.service.ComplianceService
import ke.co.smartroundclinic.doctor.domain.service.PaymentDetailsService
import ke.co.smartroundclinic.doctor.domain.service.PractitionerLicenceService
import ke.co.smartroundclinic.doctor.domain.service.PractitionerProfileService
import ke.co.smartroundclinic.doctor.domain.service.SpecializationService
import ke.co.smartroundclinic.doctor.domain.usecase.certification.AddCertificationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.DeleteCertificationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.GetCertificationByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.GetMyCertificationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.UpdateCertificationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.ApproveComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetAllComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetComplianceByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetMyComplianceStatusUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.RejectComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.SubmitComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.AddLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.DeleteLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.GetLicenceByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.GetMyLicencesUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.UpdateLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.CreatePractitionerProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetMyProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetPractitionerProfileByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.GetPractitionerProfilesUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.profile.UpdatePractitionerProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.AddPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.DeletePaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetAllPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetPaymentDetailsByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.GetPaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.payment.UpdatePaymentDetailsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.AddSpecializationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.GetMySpecializationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.specialization.RemoveSpecializationUseCase
import ke.co.smartroundclinic.doctor.data.repository.LocalBankRepositoryImpl
import ke.co.smartroundclinic.doctor.domain.repository.LocalBankRepository
import ke.co.smartroundclinic.doctor.domain.service.LocalBankService
import ke.co.smartroundclinic.doctor.domain.usecase.bank.FindLocalBankByCodeUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.FindLocalBanksByBranchCodeUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.GetAllLocalBanksUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.bank.SearchLocalBanksByNameUseCase
import ke.co.smartroundclinic.infra.AppConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

val doctorModule = module {
    /**
     * Repositories
     * */
    single<PractitionerProfileRepository> { PractitionerProfileRepositoryImpl(get(named("doctorDb"))) }
    single<CertificationRepository> { CertificationRepositoryImpl(get(named("doctorDb"))) }
    single<ComplianceRepository> { ComplianceRepositoryImpl(get(named("doctorDb"))) }
    single<PractitionerLicenceRepository> { PractitionerLicenceRepositoryImpl(get(named("doctorDb"))) }
    single<SpecializationRepository> { SpecializationRepositoryImpl(get(named("doctorDb"))) }
    single<PaymentDetailsRepository> { PaymentDetailsRepositoryImpl(get(named("doctorDb"))) }

    /**
     * Profile
     * */
    single { CreatePractitionerProfileUseCase(get()) }
    single { UpdatePractitionerProfileUseCase(get()) }
    single { GetMyProfileUseCase(get()) }
    single { GetPractitionerProfileByIdUseCase(get()) }
    single { GetPractitionerProfilesUseCase(get()) }

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
    single { AddLicenceUseCase(get(),get()) }
    single { DeleteLicenceUseCase(get(), get()) }
    single { GetLicenceByIdUseCase(get(),get()) }
    single { GetMyLicencesUseCase(get(), get()) }
    single { UpdateLicenceUseCase(get()) }

    /**
     * Compliance
     * */
    single { ApproveComplianceUseCase(get()) }
    single { GetAllComplianceUseCase(get()) }
    single { GetComplianceByIdUseCase(get()) }
    single { GetMyComplianceStatusUseCase(get()) }
    single { RejectComplianceUseCase(get()) }
    single { SubmitComplianceUseCase(get()) }

    /**
     * Specialization
     * */
    single { AddSpecializationUseCase(get()) }
    single { GetMySpecializationsUseCase(get()) }
    single { RemoveSpecializationUseCase(get()) }

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
            get()
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
            get()
        )
    }
    single {
        ComplianceService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    single {
        SpecializationService(
            get(),
            get(),
            get()
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
     * Local Banks
     * */
    single<LocalBankRepository> { LocalBankRepositoryImpl(get(named("doctorDb")), AppConfig.staticAssets.kenyanBanksJsonUrl) }
    single { GetAllLocalBanksUseCase(get()) }
    single { SearchLocalBanksByNameUseCase(get()) }
    single { FindLocalBankByCodeUseCase(get()) }
    single { FindLocalBanksByBranchCodeUseCase(get()) }
    single { LocalBankService(get(), get(), get(), get()) }
}

