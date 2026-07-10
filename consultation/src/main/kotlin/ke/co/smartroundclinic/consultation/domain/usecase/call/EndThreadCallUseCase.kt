package ke.co.smartroundclinic.consultation.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadRepository
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient

/**
 * Doctor ends the active video call for a (doctorId, patientId) thread:
 *   1. Ends the Cloudflare meeting (kicks all participants)
 *   2. Clears `videoRoomId` on the thread so the next join provisions a fresh meeting
 *
 * Purely a "hang up" — no longer tied to appointment completion (that's now a separate,
 * doctor-triggered action independent of whether/how a call happened).
 */
class EndThreadCallUseCase(
    private val threads: ConsultationThreadRepository,
    private val client: RealtimeKitClient,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(doctorId: String, patientId: String, callerId: String): DefaultResponse<Unit?> {
        if (callerId != doctorId) return DefaultResponse(
            httpStatusCode = HttpStatusCode.Forbidden.value,
            status = false,
            message = "Only the doctor can end the call",
            data = null,
        )

        val thread = when (val r = threads.getOrCreate(doctorId, patientId)) {
            is Resource.Success -> r.data ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = "Failed to load thread",
                data = null,
            )
            is Resource.Error -> return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = r.message ?: "Failed to load thread",
                data = null,
            )
        }

        val meetingId = thread.videoRoomId
        if (meetingId != null) {
            when (val result = client.endMeeting(meetingId)) {
                is Resource.Error -> return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadGateway.value,
                    status = false,
                    message = result.message ?: "Failed to end Cloudflare meeting",
                    data = null,
                )
                else -> Unit
            }
            threads.clearVideoRoomId(doctorId, patientId, completedRoomId = meetingId)
        }

        runCatching {
            notificationSender?.send(
                title = PushNotificationEvents.CALL_ENDED,
                message = "The video call has been ended by your doctor",
                channel = NotificationChannel.PUSH_NOTIFICATION,
                destination = NotificationDestination.PATIENT,
                recipientId = patientId,
                metadata = mapOf(
                    "event" to PushNotificationEvents.CALL_ENDED,
                    "doctorId" to doctorId,
                    "patientId" to patientId,
                ),
            )
        }

        return DefaultResponse(
            httpStatusCode = HttpStatusCode.OK.value,
            status = true,
            message = "Call ended",
            data = null,
        )
    }
}
