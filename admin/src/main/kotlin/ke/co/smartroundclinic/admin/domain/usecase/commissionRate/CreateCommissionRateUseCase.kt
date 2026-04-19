package ke.co.smartroundclinic.admin.domain.usecase.commissionRate

import ke.co.smartroundclinic.admin.data.entity.toEntity
import ke.co.smartroundclinic.admin.domain.repository.CommissionRateRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateCommissionRateReq
import ke.co.smartroundclinic.admin.presentation.dto.response.CommissionRateRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreateCommissionRateUseCase(private val repository: CommissionRateRepository) {
    suspend operator fun invoke(req: CreateCommissionRateReq, adminId: String): DefaultResponse<CommissionRateRes?> =
        repository.create(req.toModel(adminId).toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { it?.toModel()?.toRes() }
}
