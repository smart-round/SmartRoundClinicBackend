package ke.co.smartroundclinic.patient.koin

import ke.co.smartroundclinic.patient.data.repository.PersonalInformationRepositoryImpl
import ke.co.smartroundclinic.patient.domain.repository.PersonalInformationRepository
import ke.co.smartroundclinic.common.PatientProfileResolver
import ke.co.smartroundclinic.patient.domain.service.PersonalInformationService
import ke.co.smartroundclinic.patient.domain.usecase.CreatePersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.GetAllPersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.GetPersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.UpdatePersonalInformationUseCase
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
}
