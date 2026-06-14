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

class StartConsultationUseCase(
    private val repository: ConsultationSessionRepository,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(appointmentId: String, userId: String): DefaultResponse<ConsultationSessionRes?> {
        val resource = repository.startOrGet(appointmentId, userId)
        if (resource is Resource.Success && resource.data != null && resource.message == "Consultation started") {
            val session = resource.data!!
            val isDoctor = userId == session.doctorId
            runCatching {
                notificationSender?.send(
                    title = if (isDoctor) PushNotificationEvents.CONSULTATION_DOCTOR_READY else PushNotificationEvents.CONSULTATION_PATIENT_READY,
                    message = if (isDoctor) "Your doctor has started the consultation"
                              else "Your patient has opened the consultation",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = if (isDoctor) NotificationDestination.PATIENT else NotificationDestination.DOCTOR,
                    recipientId = if (isDoctor) session.patientId else session.doctorId,
                    metadata = mapOf("event" to if (isDoctor) PushNotificationEvents.CONSULTATION_DOCTOR_READY else PushNotificationEvents.CONSULTATION_PATIENT_READY, "consultationId" to session.id),
                )
            }
        }
        return resource.toDefaultResponse(
            successStatusCode = HttpStatusCode.Created.value,
            failedStatusCode = HttpStatusCode.BadRequest.value,
        ) { it?.toModel()?.toRes() }
    }
}
