package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class CompleteAppointmentUseCase(
    private val repository: AppointmentRepository,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<AppointmentRes?> {
        val existing = repository.getById(id)
        if (existing is Resource.Error) return existing.toDefaultResponse(failedStatusCode = 404) { null }
        val entity = existing.data ?: return Resource.Error<Nothing>("Appointment not found")
            .toDefaultResponse(failedStatusCode = 404) { null }
        if (entity.doctorId != doctorId) return Resource.Error<Nothing>("Not authorized to complete this appointment")
            .toDefaultResponse(failedStatusCode = 403) { null }

        val result = repository.updateStatus(id, "COMPLETED")
        if (result is Resource.Success && result.data != null) {
            runCatching {
                notificationSender?.send(
                    title = "Appointment Completed",
                    message = "Your appointment on ${entity.date} at ${entity.slotStart} has been completed",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.PATIENT,
                    recipientId = entity.patientId,
                )
            }
        }
        return result.toDefaultResponse { it?.toModel()?.toRes() }
    }
}
