package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class CancelAppointmentUseCase(
    private val repository: AppointmentRepository,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(
        id: String,
        userId: String,
        role: String,
        reason: String? = null,
    ): DefaultResponse<AppointmentRes?> {
        val existing = repository.getById(id)
        if (existing is Resource.Error) return existing.toDefaultResponse(failedStatusCode = 404) { null }
        val entity = existing.data ?: return Resource.Error<Nothing>("Appointment not found")
            .toDefaultResponse(failedStatusCode = 404) { null }

        val authorized = (role == "PATIENT" && entity.patientId == userId) ||
                (role == "DOCTOR" && entity.doctorId == userId)
        if (!authorized) return Resource.Error<Nothing>("Not authorized to cancel this appointment")
            .toDefaultResponse(failedStatusCode = 403) { null }

        val result = repository.updateStatus(id, "CANCELLED", cancellationReason = reason, cancelledBy = userId)
        if (result is Resource.Success && result.data != null) {
            runCatching {
                if (role == "DOCTOR") {
                    notificationSender?.send(
                        title = "Appointment Cancelled",
                        message = "Your appointment on ${entity.date} at ${entity.slotStart} has been cancelled by your doctor",
                        channel = NotificationChannel.PUSH_NOTIFICATION,
                        destination = NotificationDestination.PATIENT,
                        recipientId = entity.patientId,
                    )
                } else {
                    notificationSender?.send(
                        title = "Appointment Cancelled",
                        message = "A patient has cancelled their appointment on ${entity.date} at ${entity.slotStart}",
                        channel = NotificationChannel.PUSH_NOTIFICATION,
                        destination = NotificationDestination.DOCTOR,
                        recipientId = entity.doctorId,
                    )
                }
            }
        }
        return result.toDefaultResponse { it?.toModel()?.toRes() }
    }
}
