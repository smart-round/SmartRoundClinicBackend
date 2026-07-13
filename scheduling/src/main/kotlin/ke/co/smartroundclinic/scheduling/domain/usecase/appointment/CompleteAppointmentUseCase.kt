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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val APPOINTMENT_TIMEZONE = TimeZone.of("Africa/Nairobi")

/**
 * The doctor manually marks a CONFIRMED appointment complete once its slot has started.
 * No longer tied to call attendance — chat/calls are a permanent thread between doctor and
 * patient, independent of any single appointment, so there's nothing per-visit left to verify.
 */
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

        // Appointment slot start must be in the past
        val slotStartInstant = parseSlotTime(entity.date, entity.slotStart)
        if (slotStartInstant != null && Clock.System.now() < slotStartInstant) {
            return Resource.Error<Nothing>("Appointment cannot be completed before the scheduled start time")
                .toDefaultResponse(failedStatusCode = 422) { null }
        }

        val result = repository.updateStatus(id, "COMPLETED")
        if (result is Resource.Success && result.data != null) {
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.APPOINTMENT_COMPLETED,
                    message = "Your appointment on ${entity.date} at ${entity.slotStart} has been completed",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.PATIENT,
                    recipientId = entity.patientId,
                    metadata = mapOf("event" to PushNotificationEvents.APPOINTMENT_COMPLETED, "appointmentId" to id),
                )
            }
        }
        return result.toDefaultResponse { it?.toModel()?.toRes() }
    }

    private fun parseSlotTime(date: String, time: String): Instant? = runCatching {
        val (h, m) = time.split(":").map { it.toInt() }
        val dateParts = date.split("-").map { it.toInt() }
        LocalDateTime(dateParts[0], dateParts[1], dateParts[2], h, m, 0, 0)
            .toInstant(APPOINTMENT_TIMEZONE)
    }.getOrNull()
}
