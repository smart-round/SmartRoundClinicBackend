package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.data.repository.DoctorProfileLookup
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class ToggleMonetizationUseCase(
    private val repository: ComplianceRepository,
    private val profileLookup: DoctorProfileLookup,
) {
    suspend operator fun invoke(id: String, isMonetized: Boolean): DefaultResponse<ComplianceRes?> {
        val resource = repository.setMonetization(id, isMonetized)
        val doctorId = resource.data?.doctorId
        val profile = doctorId?.let { profileLookup.lookup(it) }
        val specializations = doctorId?.let { profileLookup.lookupSpecializations(it) } ?: emptyList()
        return resource.toDefaultResponse { entity ->
            entity?.toModel()?.toRes(profile, specializations)
        }
    }
}
