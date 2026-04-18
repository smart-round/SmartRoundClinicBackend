package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.CreateServiceTierUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.DeleteServiceTierUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.GetAllServiceTiersUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.GetServiceTierByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.serviceTier.UpdateServiceTierUseCase
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateServiceTierReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateServiceTierReq

class ServiceTierService(
    private val createServiceTierUseCase: CreateServiceTierUseCase,
    private val getServiceTierByIdUseCase: GetServiceTierByIdUseCase,
    private val getAllServiceTiersUseCase: GetAllServiceTiersUseCase,
    private val updateServiceTierUseCase: UpdateServiceTierUseCase,
    private val deleteServiceTierUseCase: DeleteServiceTierUseCase,
) {
    suspend fun create(req: CreateServiceTierReq) = createServiceTierUseCase(req)

    suspend fun getById(id: String) = getServiceTierByIdUseCase(id)

    suspend fun getAll(page: Int, size: Int) = getAllServiceTiersUseCase(page, size)

    suspend fun update(id: String, req: UpdateServiceTierReq) = updateServiceTierUseCase(id, req)

    suspend fun delete(id: String) = deleteServiceTierUseCase(id)
}
