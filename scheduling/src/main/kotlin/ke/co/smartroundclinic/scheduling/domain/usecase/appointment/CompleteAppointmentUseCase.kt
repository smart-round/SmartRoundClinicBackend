package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.realtime.MeetingParticipantRecord
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import ke.co.smartroundclinic.scheduling.data.lookup.ConsultationVideoRoomLookup
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val APPOINTMENT_TIMEZONE = TimeZone.of("Africa/Nairobi")
private const val MIN_CALL_OVERLAP_SECONDS = 120L

class CompleteAppointmentUseCase(
    private val repository: AppointmentRepository,
    private val notificationSender: NotificationSender? = null,
    private val consultationVideoRoomLookup: ConsultationVideoRoomLookup? = null,
    private val realtimeKitClient: RealtimeKitClient? = null,
) {
    suspend operator fun invoke(id: String, doctorId: String): DefaultResponse<AppointmentRes?> {
        val existing = repository.getById(id)
        if (existing is Resource.Error) return existing.toDefaultResponse(failedStatusCode = 404) { null }
        val entity = existing.data ?: return Resource.Error<Nothing>("Appointment not found")
            .toDefaultResponse(failedStatusCode = 404) { null }
        if (entity.doctorId != doctorId) return Resource.Error<Nothing>("Not authorized to complete this appointment")
            .toDefaultResponse(failedStatusCode = 403) { null }

        // Appointment slot end must be in the past
        val slotEndInstant = parseSlotEnd(entity.date, entity.slotEnd)
        if (slotEndInstant != null && Clock.System.now() < slotEndInstant) {
            return Resource.Error<Nothing>("Appointment cannot be completed before the scheduled end time")
                .toDefaultResponse(failedStatusCode = 422) { null }
        }

        // Verify the video consultation call via Cloudflare Realtime Kit
        if (consultationVideoRoomLookup != null && realtimeKitClient != null) {
            val videoRoomId = consultationVideoRoomLookup.getVideoRoomId(id)
                ?: return Resource.Error<Nothing>(
                    "No consultation call was found for this appointment. Both parties must complete the video call before marking as complete."
                ).toDefaultResponse(failedStatusCode = 422) { null }

            val participants = realtimeKitClient.getParticipants(videoRoomId)

            val doctorJoined = participants.any { it.customParticipantId == entity.doctorId && it.joinedAt != null }
            val patientJoined = participants.any { it.customParticipantId == entity.patientId && it.joinedAt != null }

            if (!doctorJoined || !patientJoined) {
                val missing = buildList {
                    if (!doctorJoined) add("doctor")
                    if (!patientJoined) add("patient")
                }.joinToString(" and ")
                return Resource.Error<Nothing>("Both parties must join the consultation call before completing the appointment. Missing: $missing.")
                    .toDefaultResponse(failedStatusCode = 422) { null }
            }

            val overlapSeconds = computeCallOverlapSeconds(participants, entity.doctorId, entity.patientId)
            if (overlapSeconds < MIN_CALL_OVERLAP_SECONDS) {
                val mins = overlapSeconds / 60
                val secs = overlapSeconds % 60
                return Resource.Error<Nothing>(
                    "Consultation call must last at least 2 minutes. Actual overlap: ${mins}m ${secs}s."
                ).toDefaultResponse(failedStatusCode = 422) { null }
            }
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

    private fun parseSlotEnd(date: String, slotEnd: String): Instant? = runCatching {
        val (h, m) = slotEnd.split(":").map { it.toInt() }
        val dateParts = date.split("-").map { it.toInt() }
        LocalDateTime(dateParts[0], dateParts[1], dateParts[2], h, m, 0, 0)
            .toInstant(APPOINTMENT_TIMEZONE)
    }.getOrNull()

    private fun computeCallOverlapSeconds(
        participants: List<MeetingParticipantRecord>,
        doctorId: String,
        patientId: String,
    ): Long {
        val nowEpoch = Clock.System.now().epochSeconds

        fun intervals(userId: String): List<Pair<Long, Long>> = participants
            .filter { it.customParticipantId == userId && it.joinedAt != null }
            .mapNotNull { p ->
                val joinEpoch = runCatching { Instant.parse(p.joinedAt!!).epochSeconds }.getOrNull()
                    ?: return@mapNotNull null
                val leaveEpoch = p.leftAt
                    ?.let { runCatching { Instant.parse(it).epochSeconds }.getOrNull() }
                    ?: nowEpoch
                joinEpoch to leaveEpoch
            }

        val doctorIntervals = intervals(doctorId)
        val patientIntervals = intervals(patientId)

        var totalSeconds = 0L
        for ((dJoin, dLeave) in doctorIntervals) {
            for ((pJoin, pLeave) in patientIntervals) {
                val overlapStart = if (dJoin > pJoin) dJoin else pJoin
                val overlapEnd = if (dLeave < pLeave) dLeave else pLeave
                if (overlapEnd > overlapStart) {
                    totalSeconds += overlapEnd - overlapStart
                }
            }
        }
        return totalSeconds
    }
}
