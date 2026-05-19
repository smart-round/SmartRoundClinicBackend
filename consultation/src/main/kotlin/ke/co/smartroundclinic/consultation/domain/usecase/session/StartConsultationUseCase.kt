package ke.co.smartroundclinic.consultation.domain.usecase.session

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationSessionRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes

class StartConsultationUseCase(private val repository: ConsultationSessionRepository) {
    suspend operator fun invoke(appointmentId: String, userId: String): DefaultResponse<ConsultationSessionRes?> =
        repository.startOrGet(appointmentId, userId).toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadRequest.value,
        ) { it?.toModel()?.toRes() }
}
