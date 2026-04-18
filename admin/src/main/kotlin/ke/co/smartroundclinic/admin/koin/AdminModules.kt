package ke.co.smartroundclinic.admin.koin

import ke.co.smartroundclinic.admin.data.repository.KmpdcRepositoryImpl
import ke.co.smartroundclinic.admin.data.repository.ServiceTierRepositoryImpl
import ke.co.smartroundclinic.admin.data.repository.SpecialityRepositoryImpl
import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.domain.service.KmpdcService
import ke.co.smartroundclinic.admin.domain.service.ServiceTierService
import ke.co.smartroundclinic.admin.domain.service.SpecialityService
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.CreateServiceTierUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.DeleteServiceTierUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.GetAllServiceTiersUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.GetServiceTierByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.UpdateServiceTierUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.CreateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.CreateSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.DeleteSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.DeleteSubSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.kmpdc.FindKmpdcByRegNumberUseCase
import ke.co.smartroundclinic.admin.domain.usecase.kmpdc.GetKmpdcPractitionersUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.RemoveSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.RemoveSubSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.kmpdc.RefreshKmpdcRegisterUseCase
import ke.co.smartroundclinic.admin.domain.usecase.kmpdc.SearchKmpdcByNameUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.UploadSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.UploadSubSpecialityIconUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.GetSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.GetSpecialityByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.GetSubSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.UpdateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.UpdateSubSpecialityUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val adminModule = module {
    single<SpecialityRepository> { SpecialityRepositoryImpl(get(named("adminDb"))) }
    single<KmpdcRepository> { KmpdcRepositoryImpl(get(named("adminDb"))) }
    single<ServiceTierRepository> { ServiceTierRepositoryImpl(get(named("adminDb"))) }

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

    // Service Tiers
    single { CreateServiceTierUseCase(get()) }
    single { GetServiceTierByIdUseCase(get()) }
    single { GetAllServiceTiersUseCase(get()) }
    single { UpdateServiceTierUseCase(get()) }
    single { DeleteServiceTierUseCase(get()) }
    single { ServiceTierService(get(), get(), get(), get(), get()) }

    // KMPDC
    single { RefreshKmpdcRegisterUseCase(get()) }
    single { GetKmpdcPractitionersUseCase(get()) }
    single { FindKmpdcByRegNumberUseCase(get()) }
    single { SearchKmpdcByNameUseCase(get()) }
    single { KmpdcService(get(), get(), get(), get()) }

}
