package ke.co.smartroundclinic.admin.koin

import ke.co.smartroundclinic.admin.data.repository.SpecialityRepositoryImpl
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.domain.service.SpecialityService
import ke.co.smartroundclinic.admin.domain.usecase.CreateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.CreateSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.DeleteSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSpecialityByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSubSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.UpdateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.UpdateSubSpecialityUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val adminModule = module {
    single<SpecialityRepository> { SpecialityRepositoryImpl(get(named("adminDb"))) }

    single { CreateSpecialityUseCase(get()) }
    single { UpdateSpecialityUseCase(get()) }
    single { GetSpecialitiesUseCase(get()) }
    single { GetSpecialityByIdUseCase(get()) }
    single { CreateSubSpecialityUseCase(get()) }
    single { UpdateSubSpecialityUseCase(get()) }
    single { GetSubSpecialitiesUseCase(get()) }
    single { DeleteSubSpecialityUseCase(get()) }

    single {
        SpecialityService(
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
}
