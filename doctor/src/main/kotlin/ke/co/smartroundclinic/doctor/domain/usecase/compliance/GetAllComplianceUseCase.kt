package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.repository.DoctorProfileLookup
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.CompliancePageResult
import ke.co.smartroundclinic.doctor.presentation.dto.response.DoctorProfileInfo
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes
import kotlin.math.ceil

class GetAllComplianceUseCase(
    private val repository: ComplianceRepository,
    private val profileLookup: DoctorProfileLookup,
) {
    suspend operator fun invoke(page: Int, size: Int): DefaultResponse<CompliancePageResult?> {
        val resource = repository.getAll(page, size)
        val doctorIds: Set<String> = if (resource is Resource.Success && resource.data != null)
            resource.data!!.first.map { it.doctorId }.toSet()
        else emptySet()

        val profiles: Map<String, DoctorProfileInfo> = if (doctorIds.isNotEmpty())
            profileLookup.bulkLookup(doctorIds) else emptyMap()
        val specializations = if (doctorIds.isNotEmpty())
            profileLookup.bulkLookupSpecializations(doctorIds) else emptyMap()

        return resource.toDefaultResponse { pair ->
            pair?.let { (items, total) ->
                CompliancePageResult(
                    items = items.map { it.toModel().toRes(profiles[it.doctorId], specializations[it.doctorId] ?: emptyList()) },
                    total = total,
                    page = page,
                    size = size,
                    pages = ceil(total.toDouble() / size).toLong(),
                )
            }
        }
    }
}
