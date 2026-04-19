package ke.co.smartroundclinic.admin.domain.usecase.commissionRate

import ke.co.smartroundclinic.admin.domain.repository.CommissionRateRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdateCommissionRateReq
import ke.co.smartroundclinic.admin.presentation.dto.response.CommissionRateRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class UpdateCommissionRateUseCase(private val repository: CommissionRateRepository) {
    suspend operator fun invoke(id: String, req: UpdateCommissionRateReq): DefaultResponse<CommissionRateRes?> =
        repository.update(id, req.commissionRate)
            .toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
