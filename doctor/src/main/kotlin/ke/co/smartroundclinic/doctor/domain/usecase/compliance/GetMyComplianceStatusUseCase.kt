package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceCorrectionRepository
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

class GetMyComplianceStatusUseCase(
    private val repository: ComplianceRepository,
    private val correctionRepository: ComplianceCorrectionRepository,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<ComplianceRes?> {
        val hasPendingCorrection = correctionRepository.hasPending(doctorId)
        return repository.getByDoctorId(doctorId).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
        ) { it?.toModel()?.toRes(hasPendingCorrection = hasPendingCorrection) }.let { response ->
            if (response.status && response.data == null)
                response.copy(
                    httpStatusCode = HttpStatusCode.NotFound.value,
                    status = false,
                    message = "No compliance record found. Please submit for review.",
                )
            else response
        }
    }
}