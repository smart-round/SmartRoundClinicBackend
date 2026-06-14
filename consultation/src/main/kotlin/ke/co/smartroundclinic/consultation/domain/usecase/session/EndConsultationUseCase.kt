package ke.co.smartroundclinic.consultation.domain.usecase.session

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationSessionRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes

class EndConsultationUseCase(
    private val repository: ConsultationSessionRepository,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<ConsultationSessionRes?> {
        val result = repository.end(id, doctorId)
        if (result is Resource.Success && result.data != null) {
            val session = result.data!!
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.CONSULTATION_ENDED,
                    message = "Your doctor has ended the consultation",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.PATIENT,
                    recipientId = session.patientId,
                    metadata = mapOf(
                        "event" to PushNotificationEvents.CONSULTATION_ENDED,
                        "consultationId" to id,
                        "appointmentId" to session.appointmentId,
                    ),
                )
            }
        }
        return result.toDefaultResponse(
            failedStatusCode = HttpStatusCode.InternalServerError.value,
            successMessage = "Consultation ended",
        ) { it?.toModel()?.toRes() }.let { response ->
            if (response.status && response.data == null)
                response.copy(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Consultation not found")
            else response
        }
    }
}
