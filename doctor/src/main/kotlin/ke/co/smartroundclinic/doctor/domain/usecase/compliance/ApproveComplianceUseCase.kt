package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class ApproveComplianceUseCase(private val repository: ComplianceRepository) {
    suspend operator fun invoke(id: String, adminId: String): DefaultResponse<ComplianceRes?> =
        repository.approve(id, adminId).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            successMessage = "Doctor approved successfully",
        ) { it?.toModel()?.toRes() }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "Compliance record not found",
                )
            else response
        }
}