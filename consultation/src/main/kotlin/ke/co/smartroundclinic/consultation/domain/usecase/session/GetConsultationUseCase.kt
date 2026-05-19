package ke.co.smartroundclinic.consultation.domain.usecase.session

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationSessionRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes

class GetConsultationUseCase(private val repository: ConsultationSessionRepository) {
    suspend fun byId(id: String): DefaultResponse<ConsultationSessionRes?> =
        repository.getById(id).toDefaultResponse { entity ->
            entity?.toModel()?.toRes()
        }.let { response ->
            if (response.status && response.data == null)
                response.copy(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Consultation not found")
            else response
        }

    suspend fun byAppointment(appointmentId: String): DefaultResponse<ConsultationSessionRes?> =
        repository.getByAppointmentId(appointmentId).toDefaultResponse { entity ->
            entity?.toModel()?.toRes()
        }.let { response ->
            if (response.status && response.data == null)
                response.copy(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Consultation not found")
            else response
        }
}
