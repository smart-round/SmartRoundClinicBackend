package ke.co.smartroundclinic.admin.koin

import com.mongodb.kotlin.client.coroutine.MongoClient
import ke.co.smartroundclinic.admin.data.repository.CommissionRateRepositoryImpl
import ke.co.smartroundclinic.admin.data.repository.KmpdcRepositoryImpl
import ke.co.smartroundclinic.admin.data.repository.PermissionCatalogRepositoryImpl
import ke.co.smartroundclinic.admin.data.repository.PolicyGroupRepositoryImpl
import ke.co.smartroundclinic.admin.data.repository.ServiceTierRepositoryImpl
import ke.co.smartroundclinic.admin.data.repository.SpecialityRepositoryImpl
import ke.co.smartroundclinic.admin.domain.repository.CommissionRateRepository
import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.admin.domain.repository.PermissionCatalogRepository
import ke.co.smartroundclinic.admin.domain.repository.PolicyGroupRepository
import ke.co.smartroundclinic.admin.domain.repository.ServiceTierRepository
import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.admin.domain.service.CommissionRateService
import ke.co.smartroundclinic.admin.domain.service.KmpdcService
import ke.co.smartroundclinic.admin.domain.service.PolicyGroupService
import ke.co.smartroundclinic.admin.domain.service.ServiceTierService
import ke.co.smartroundclinic.admin.domain.service.SpecialityService
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.CreateCommissionRateUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.DeleteCommissionRateUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.GetCommissionRateByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.UpdateCommissionRateUseCase
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
import ke.co.smartroundclinic.admin.domain.usecase.kmpdc.RefreshKmpdcRegisterUseCase
import ke.co.smartroundclinic.admin.domain.usecase.kmpdc.SearchKmpdcByNameUseCase
import ke.co.smartroundclinic.admin.domain.usecase.permissionCatalog.GetPermissionCatalogUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.AssignAdminToPolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.CreatePolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.DeletePolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.GetPolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.ListPolicyGroupsUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.RemoveAdminFromPolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.ReplaceAdminPolicyGroupsUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.UpdatePolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.GetSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.GetSpecialityByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.GetSubSpecialitiesUseCase
import ke.co.smartroundclinic.admin.domain.usecase.speciality.UpdateSpecialityUseCase
import ke.co.smartroundclinic.admin.domain.usecase.subSpeciality.UpdateSubSpecialityUseCase
import ke.co.smartroundclinic.common.PolicyGroupPermissionResolver
import org.koin.core.qualifier.named
import org.koin.dsl.module

val adminModule = module {
    single<SpecialityRepository> { SpecialityRepositoryImpl(get<MongoClient>(), get(named("adminDb"))) }
    single<KmpdcRepository> { KmpdcRepositoryImpl(get(named("adminDb"))) }
    single<ServiceTierRepository> { ServiceTierRepositoryImpl(get(named("adminDb"))) }
    single<CommissionRateRepository> { CommissionRateRepositoryImpl(get(named("adminDb"))) }

    single { PolicyGroupRepositoryImpl(get(named("adminDb")), get(named("authDb"))) }
    single<PolicyGroupRepository> { get<PolicyGroupRepositoryImpl>() }
    single<PolicyGroupPermissionResolver> { get<PolicyGroupRepositoryImpl>() }

    single<PermissionCatalogRepository> { PermissionCatalogRepositoryImpl(get(named("adminDb"))) }
    single { GetPermissionCatalogUseCase(get()) }

    single { CreatePolicyGroupUseCase(get()) }
    single { GetPolicyGroupUseCase(get(), get()) }
    single { ListPolicyGroupsUseCase(get(), get()) }
    single { UpdatePolicyGroupUseCase(get()) }
    single { DeletePolicyGroupUseCase(get()) }
    single { AssignAdminToPolicyGroupUseCase(get()) }
    single { RemoveAdminFromPolicyGroupUseCase(get()) }
    single { ReplaceAdminPolicyGroupsUseCase(get()) }
    single { PolicyGroupService(get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    single { CreateSpecialityUseCase(get(), get()) }
    single { UpdateSpecialityUseCase(get(), get()) }
    single { GetSpecialitiesUseCase(get()) }
    single { GetSpecialityByIdUseCase(get()) }
    single { CreateSubSpecialityUseCase(get(), get()) }
    single { UpdateSubSpecialityUseCase(get(), get()) }
    single { GetSubSpecialitiesUseCase(get()) }
    single { DeleteSpecialityUseCase(get()) }
    single { DeleteSubSpecialityUseCase(get()) }

    single { SpecialityService(get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    // Service Tiers
    single { CreateServiceTierUseCase(get()) }
    single { GetServiceTierByIdUseCase(get()) }
    single { GetAllServiceTiersUseCase(get()) }
    single { UpdateServiceTierUseCase(get()) }
    single { DeleteServiceTierUseCase(get()) }
    single { ServiceTierService(get(), get(), get(), get(), get()) }

    // Commission Rates
    single { CreateCommissionRateUseCase(get()) }
    single { GetCommissionRateByIdUseCase(get()) }
    single { UpdateCommissionRateUseCase(get()) }
    single { DeleteCommissionRateUseCase(get()) }
    single { CommissionRateService(get(), get(), get(), get()) }

    // KMPDC
    single { RefreshKmpdcRegisterUseCase(get()) }
    single { GetKmpdcPractitionersUseCase(get()) }
    single { FindKmpdcByRegNumberUseCase(get()) }
    single { SearchKmpdcByNameUseCase(get()) }
    single { KmpdcService(get(), get(), get(), get()) }

}
