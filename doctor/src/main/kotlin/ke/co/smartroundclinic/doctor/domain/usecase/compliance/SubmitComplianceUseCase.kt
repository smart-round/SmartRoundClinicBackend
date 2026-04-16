package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class SubmitComplianceUseCase(
    private val repository: ComplianceRepository
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<ComplianceRes?> =
        repository.submit(doctorId).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.Conflict.value,
            successMessage = "Submitted for review successfully",
        ) { it?.toModel()?.toRes() }
}
