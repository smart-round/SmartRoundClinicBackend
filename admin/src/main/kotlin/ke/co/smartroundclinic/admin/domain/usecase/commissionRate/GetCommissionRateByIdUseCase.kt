package ke.co.smartroundclinic.admin.domain.usecase.commissionRate

import ke.co.smartroundclinic.admin.domain.repository.CommissionRateRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.CommissionRateRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetCommissionRateByIdUseCase(private val repository: CommissionRateRepository) {
    suspend operator fun invoke(): DefaultResponse<CommissionRateRes?> =
        repository.getById()
            .toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
