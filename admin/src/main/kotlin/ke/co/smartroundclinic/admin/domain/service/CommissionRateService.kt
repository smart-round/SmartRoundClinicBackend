package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.CreateCommissionRateUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.DeleteCommissionRateUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.GetCommissionRateByIdUseCase
import ke.co.smartroundclinic.admin.domain.usecase.commissionRate.UpsertCommissionRateUseCase
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateCommissionRateReq

class CommissionRateService(
    private val createUseCase: CreateCommissionRateUseCase,
    private val getByIdUseCase: GetCommissionRateByIdUseCase,
    private val deleteUseCase: DeleteCommissionRateUseCase,
    private val upsertUseCase: UpsertCommissionRateUseCase,
) {
    suspend fun create(req: CreateCommissionRateReq, adminId: String) = createUseCase(req, adminId)
    suspend fun getById() = getByIdUseCase()
    suspend fun delete(id: String) = deleteUseCase(id)
    suspend fun upsert(req: CreateCommissionRateReq, adminId: String) = upsertUseCase(req, adminId)
}
