package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.ComplianceCorrectionEntity
import ke.co.smartroundclinic.doctor.data.repository.DoctorProfileLookup
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceCorrectionRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceCorrectionsPageRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

/** Admin queue view — most recently submitted corrections across all doctors, newest first. */
class GetLatestComplianceCorrectionsUseCase(
    private val repository: ComplianceCorrectionRepository,
    private val profileLookup: DoctorProfileLookup,
) {
    suspend operator fun invoke(page: Int, size: Int, status: String? = null): DefaultResponse<ComplianceCorrectionsPageRes?> {
        val safePage = maxOf(1, page)
        val safeSize = minOf(maxOf(1, size), 100)
        val resource = repository.getLatest(safePage, safeSize, status)

        val doctorIds: Set<String> = if (resource is Resource.Success && resource.data != null)
            resource.data!!.first.map { it.doctorId }.toSet()
        else emptySet()
        val profiles = if (doctorIds.isNotEmpty()) profileLookup.bulkLookup(doctorIds) else emptyMap()

        return resource.toDefaultResponse(failedStatusCode = 500) {
            pair: Pair<List<ComplianceCorrectionEntity>, Long>? ->
            val total = pair?.second ?: 0L
            ComplianceCorrectionsPageRes(
                items = pair?.first.orEmpty().map { it.toModel().toRes(profiles[it.doctorId]) },
                total = total,
                page = safePage,
                size = safeSize,
                pages = if (total == 0L) 0L else ceil(total.toDouble() / safeSize).toLong(),
            )
        }
    }
}
