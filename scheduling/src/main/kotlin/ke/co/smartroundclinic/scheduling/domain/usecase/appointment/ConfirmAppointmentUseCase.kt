package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes
import org.slf4j.LoggerFactory

class ConfirmAppointmentUseCase(
    private val repository: AppointmentRepository,
    private val notificationSender: NotificationSender? = null,
) {
    private val log = LoggerFactory.getLogger(ConfirmAppointmentUseCase::class.java)

    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<AppointmentRes?> {
        val existing = repository.getById(id)
        if (existing is Resource.Error) return existing.toDefaultResponse(failedStatusCode = 404) { null }
        val entity = existing.data ?: return Resource.Error<Nothing>("Appointment not found")
            .toDefaultResponse(failedStatusCode = 404) { null }
        if (entity.doctorId != doctorId) return Resource.Error<Nothing>("Not authorized to confirm this appointment")
            .toDefaultResponse(failedStatusCode = 403) { null }

        val result = repository.updateStatus(id, "CONFIRMED")
        if (result is Resource.Success && result.data != null) {
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.APPOINTMENT_CONFIRMED,
                    message = "Your appointment on ${entity.date} at ${entity.slotStart} has been confirmed by your doctor",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.PATIENT,
                    recipientId = entity.patientId,
                    metadata = mapOf("event" to PushNotificationEvents.APPOINTMENT_CONFIRMED, "appointmentId" to id),
                )
            }.onFailure { e -> log.error("Appointment-confirmed push notification threw for appointmentId=$id", e) }
        }
        return result.toDefaultResponse { it?.toModel()?.toRes() }
    }
}
