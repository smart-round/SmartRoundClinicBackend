package ke.co.smartroundclinic.admin.koin

import ke.co.smartroundclinic.admin.data.repository.KmpdcRepositoryImpl
import ke.co.smartroundclinic.admin.data.repository.SpecialityRepositoryImpl
import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.domain.service.KmpdcService
import ke.co.smartroundclinic.admin.domain.service.SpecialityService
import ke.co.smartroundclinic.admin.domain.usecase.CreateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.CreateSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.DeleteSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.DeleteSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.FindKmpdcByRegNumberUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetKmpdcPractitionersUseCase
import ke.co.smartroundclinic.admin.domain.usecase.RemoveSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.RemoveSubSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.RefreshKmpdcRegisterUseCase
import ke.co.smartroundclinic.admin.domain.usecase.SearchKmpdcByNameUseCase
import ke.co.smartroundclinic.admin.domain.usecase.UploadSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.UploadSubSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSpecialityByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.GetSubSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.UpdateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.UpdateSubSpecialityUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val adminModule = module {
    single<SpecialityRepository> { SpecialityRepositoryImpl(get(named("adminDb"))) }
    single<KmpdcRepository> { KmpdcRepositoryImpl(get(named("adminDb"))) }

    single { CreateSpecialityUseCase(get()) }
    single { UpdateSpecialityUseCase(get()) }
    single { GetSpecialitiesUseCase(get()) }
    single { GetSpecialityByIdUseCase(get()) }
    single { CreateSubSpecialityUseCase(get()) }
    single { UpdateSubSpecialityUseCase(get()) }
    single { GetSubSpecialitiesUseCase(get()) }
    single { DeleteSpecialityUseCase(get()) }
    single { DeleteSubSpecialityUseCase(get()) }
    single { UploadSpecialityIconUseCase(get(), get()) }
    single { RemoveSpecialityIconUseCase(get(), get()) }
    single { UploadSubSpecialityIconUseCase(get(), get()) }
    single { RemoveSubSpecialityIconUseCase(get(), get()) }

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
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    // KMPDC
    single { RefreshKmpdcRegisterUseCase(get()) }
    single { GetKmpdcPractitionersUseCase(get()) }
    single { FindKmpdcByRegNumberUseCase(get()) }
    single { SearchKmpdcByNameUseCase(get()) }
    single { KmpdcService(get(), get(), get(), get()) }
}
