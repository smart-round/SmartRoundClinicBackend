package ke.co.smartroundclinic.admin.domain.usecase.commissionRate

import ke.co.smartroundclinic.admin.domain.repository.CommissionRateRepository
import ke.co.smartroundclinic.admin.presentation.dto.request.CreateCommissionRateReq
import ke.co.smartroundclinic.admin.presentation.dto.response.CommissionRateRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

/** Creates the single platform-wide commission rate if none exists yet, otherwise updates its rate. */
class UpsertCommissionRateUseCase(private val repository: CommissionRateRepository) {
    suspend operator fun invoke(req: CreateCommissionRateReq, adminId: String): DefaultResponse<CommissionRateRes?> =
        repository.upsert(req.commissionRate, adminId)
            .toDefaultResponse(successStatusCode = 200, failedStatusCode = 400) { it?.toModel()?.toRes() }
}
