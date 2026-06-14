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

class MarkNoShowUseCase(
    private val repository: AppointmentRepository,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<AppointmentRes?> {
        val existing = repository.getById(id)
        if (existing is Resource.Error) return existing.toDefaultResponse(failedStatusCode = 404) { null }
        val entity = existing.data ?: return Resource.Error<Nothing>("Appointment not found")
            .toDefaultResponse(failedStatusCode = 404) { null }
        if (entity.doctorId != doctorId) return Resource.Error<Nothing>("Not authorized to mark this appointment")
            .toDefaultResponse(failedStatusCode = 403) { null }

        val result = repository.updateStatus(id, "NO_SHOW")
        if (result is Resource.Success && result.data != null) {
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.APPOINTMENT_NO_SHOW,
                    message = "You were marked as a no-show for your appointment on ${entity.date} at ${entity.slotStart}",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.PATIENT,
                    recipientId = entity.patientId,
                    metadata = mapOf("event" to PushNotificationEvents.APPOINTMENT_NO_SHOW, "appointmentId" to id),
                )
            }
        }
        return result.toDefaultResponse { it?.toModel()?.toRes() }
    }
}
