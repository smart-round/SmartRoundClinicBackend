package ke.co.smartroundclinic.doctor.koin

import ke.co.smartroundclinic.doctor.data.repository.PractitionerProfileRepositoryImpl
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.domain.service.PractitionerProfileService
import ke.co.smartroundclinic.doctor.domain.usecase.CreatePractitionerProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.GetMyProfileUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.GetPractitionerProfileByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.GetPractitionerProfilesUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.UpdatePractitionerProfileUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val doctorModule = module {
    single<PractitionerProfileRepository> { PractitionerProfileRepositoryImpl(get(named("doctorDb"))) }

    single { CreatePractitionerProfileUseCase(get()) }
    single { UpdatePractitionerProfileUseCase(get()) }
    single { GetMyProfileUseCase(get()) }
    single { GetPractitionerProfileByIdUseCase(get()) }
    single { GetPractitionerProfilesUseCase(get()) }

    single {
        PractitionerProfileService(get(), get(), get(), get(), get())
    }
}