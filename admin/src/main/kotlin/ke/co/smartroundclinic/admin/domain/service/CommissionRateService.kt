package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.CreateCommissionRateUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.DeleteCommissionRateUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.GetCommissionRateByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.UpdateCommissionRateUseCase
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateCommissionRateReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateCommissionRateReq

class CommissionRateService(
    private val createUseCase: CreateCommissionRateUseCase,
    private val getByIdUseCase: GetCommissionRateByIdUseCase,
    private val updateUseCase: UpdateCommissionRateUseCase,
    private val deleteUseCase: DeleteCommissionRateUseCase,
) {
    suspend fun create(req: CreateCommissionRateReq, adminId: String) = createUseCase(req, adminId)
    suspend fun getById() = getByIdUseCase()
    suspend fun update(id: String, req: UpdateCommissionRateReq) = updateUseCase(id, req)
    suspend fun delete(id: String) = deleteUseCase(id)
}
