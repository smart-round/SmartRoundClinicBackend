package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetComplianceByIdUseCase(private val repository: ComplianceRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<ComplianceRes?> =
        repository.getById(id).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
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
