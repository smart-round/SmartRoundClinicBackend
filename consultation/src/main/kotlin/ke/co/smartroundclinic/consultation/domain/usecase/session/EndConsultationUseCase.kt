package ke.co.smartroundclinic.consultation.domain.usecase.session

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationSessionRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes

class EndConsultationUseCase(private val repository: ConsultationSessionRepository) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<ConsultationSessionRes?> =
        repository.end(id, doctorId).toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            successMessage = "Consultation ended",
        ) { it?.toModel()?.toRes() }.let { response ->
            if (response.status && response.data == null)
                response.copy(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Consultation not found")
            else response
        }
}
