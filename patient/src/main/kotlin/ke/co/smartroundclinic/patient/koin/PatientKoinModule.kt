package ke.co.smartroundclinic.patient.koin

import ke.co.smartroundclinic.patient.data.repository.PatientRatingRepositoryImpl
import ke.co.smartroundclinic.patient.data.repository.PersonalInformationRepositoryImpl
import ke.co.smartroundclinic.patient.domain.repository.PatientRatingRepository
import ke.co.smartroundclinic.patient.domain.repository.PersonalInformationRepository
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.PatientProfileResolver
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.patient.domain.service.PatientRatingService
import ke.co.smartroundclinic.patient.domain.service.PersonalInformationService
import ke.co.smartroundclinic.patient.domain.usecase.CreatePersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.GetAllPersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.GetPersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.UpdatePersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.DeletePatientRatingUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.GetPatientRatingByIdUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.GetPatientRatingsUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.SubmitPatientRatingUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.UpdatePatientRatingUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val patientModule = module {
    single<PersonalInformationRepositoryImpl> { PersonalInformationRepositoryImpl(get(named("patientDb"))) }
    single<PersonalInformationRepository> { get<PersonalInformationRepositoryImpl>() }
    single<PatientProfileResolver> { get<PersonalInformationRepositoryImpl>() }
    single { CreatePersonalInformationUseCase(get()) }
    single { GetPersonalInformationUseCase(get()) }
    single { GetAllPersonalInformationUseCase(get()) }
    single { UpdatePersonalInformationUseCase(get()) }
    single { PersonalInformationService(get(), get(), get(), get()) }

    single<PatientRatingRepository> { PatientRatingRepositoryImpl(get(named("patientDb")), get(named("schedulingDb"))) }
    single { SubmitPatientRatingUseCase(get()) }
    single { UpdatePatientRatingUseCase(get()) }
    single { DeletePatientRatingUseCase(get()) }
    single { GetPatientRatingsUseCase(get(), getOrNull<PatientNameResolver>(), getOrNull<UserProfilePictureResolver>()) }
    single { GetPatientRatingByIdUseCase(get()) }
    single { PatientRatingService(get(), get(), get(), get(), get()) }
}
