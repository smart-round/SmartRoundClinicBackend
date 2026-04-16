package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.CompliancePageResult
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

class GetAllComplianceUseCase(private val repository: ComplianceRepository) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<CompliancePageResult?> =
        repository.getAll(page, size).toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                CompliancePageResult(
                    items = items.map { it.toModel().toRes() },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
}
