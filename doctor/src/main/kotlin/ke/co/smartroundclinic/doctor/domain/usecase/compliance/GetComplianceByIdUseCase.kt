package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.repository.DoctorProfileLookup
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.DoctorProfileInfo
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetComplianceByIdUseCase(
    private val repository: ComplianceRepository,
    private val profileLookup: DoctorProfileLookup,
) {
    suspend operator fun invoke(id: String): DefaultResponse<ComplianceRes?> {
        val resource = repository.getById(id)
        val profile: DoctorProfileInfo? = if (resource is Resource.Success && resource.data != null) {
            profileLookup.lookup(resource.data!!.doctorId)
        } else null

        return resource.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
        ) { entity ->
            entity?.toModel()?.toRes(profile)
        }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Compliance record not found",
                )
            else response
        }
    }
}
