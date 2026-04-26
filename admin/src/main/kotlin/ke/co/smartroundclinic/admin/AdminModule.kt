package ke.co.smartroundclinic.admin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.admin.domain.service.CommissionRateService
import ke.co.smartroundclinic.admin.domain.service.KmpdcService
import ke.co.smartroundclinic.admin.domain.service.PolicyGroupService
import ke.co.smartroundclinic.admin.domain.service.ServiceCategoryService
import ke.co.smartroundclinic.admin.domain.service.ServiceTierService
import ke.co.smartroundclinic.admin.domain.service.SpecialityService
import ke.co.smartroundclinic.admin.presentation.controller.commissionRateController
import ke.co.smartroundclinic.admin.presentation.controller.kmpdcController
import ke.co.smartroundclinic.admin.presentation.controller.policyGroupController
import ke.co.smartroundclinic.admin.presentation.controller.serviceCategoryController
import ke.co.smartroundclinic.admin.presentation.controller.serviceTierController
import ke.co.smartroundclinic.admin.presentation.controller.specialityController
import org.koin.ktor.ext.inject

fun Application.adminModule() {
    val specialityService: SpecialityService by inject()
    val kmpdcService: KmpdcService by inject()
    val serviceTierService: ServiceTierService by inject()
    val serviceCategoryService: ServiceCategoryService by inject()
    val commissionRateService: CommissionRateService by inject()
    val policyGroupService: PolicyGroupService by inject()
    routing {
        specialityController(specialityService)
        kmpdcController(kmpdcService)
        serviceTierController(serviceTierService)
        serviceCategoryController(serviceCategoryService)
        commissionRateController(commissionRateService)
        policyGroupController(policyGroupService)
    }
}
