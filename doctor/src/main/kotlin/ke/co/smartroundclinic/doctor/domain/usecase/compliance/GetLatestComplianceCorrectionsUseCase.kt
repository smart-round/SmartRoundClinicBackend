package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.data.entity.ComplianceCorrectionEntity
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceCorrectionRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceCorrectionsPageRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

/** Admin queue view — most recently submitted corrections across all doctors, newest first. */
class GetLatestComplianceCorrectionsUseCase(private val repository: ComplianceCorrectionRepository) {
    suspend operator fun invoke(page: Int, size: Int, status: String? = null): DefaultResponse<ComplianceCorrectionsPageRes?> {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        return repository.getLatest(safePage, safeSize, status).toDefaultResponse(failedStatusCode = 500) {
            pair: Pair<List<ComplianceCorrectionEntity>, Long>? ->
            val total = pair?.second ?: 0L
            ComplianceCorrectionsPageRes(
                items = pair?.first.orEmpty().map { it.toModel().toRes() },
                total = total,
                page = safePage,
                size = safeSize,
                pages = if (total == 0L) 0L else ceil(total.toDouble() / safeSize).toLong(),
            )
        }
    }
}
