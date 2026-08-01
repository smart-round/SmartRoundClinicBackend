package ke.co.smartroundclinic.doctorchat.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient

/**
 * Either participant can end a doctor-chat call (unlike consultation's EndThreadCallUseCase, which
 * restricts this to the doctor — here both sides are doctors, so there's no asymmetry to preserve).
 */
class EndDoctorCallUseCase(
    private val threads: DoctorChatThreadRepository,
    private val client: RealtimeKitClient,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(threadId: String, callerId: String): DefaultResponse<Unit?> {
        val thread = when (val r = threads.getById(threadId)) {
            is Resource.Success -> r.data ?: return DefaultResponse(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Thread not found", data = null)
            is Resource.Error -> return DefaultResponse(httpStatusCode = HttpStatusCode.InternalServerError.value, status = false, message = r.message ?: "Failed to load thread", data = null)
        }
        if (callerId != thread.doctorAId && callerId != thread.doctorBId) {
            return DefaultResponse(httpStatusCode = HttpStatusCode.Forbidden.value, status = false, message = "Not a participant of this thread", data = null)
        }

        val meetingId = thread.videoRoomId
        if (meetingId != null) {
            when (val result = client.endMeeting(meetingId)) {
                is Resource.Error -> return DefaultResponse(httpStatusCode = HttpStatusCode.BadGateway.value, status = false, message = result.message ?: "Failed to end Cloudflare meeting", data = null)
                else -> Unit
            }
            threads.clearVideoRoomId(threadId, completedRoomId = meetingId)
        }

        val otherDoctorId = if (callerId == thread.doctorAId) thread.doctorBId else thread.doctorAId
        runCatching {
            notificationSender?.send(
                title = PushNotificationEvents.CALL_ENDED,
                message = "The call has ended",
                channel = NotificationChannel.PUSH_NOTIFICATION,
                destination = NotificationDestination.DOCTOR,
                recipientId = otherDoctorId,
                metadata = mapOf("event" to PushNotificationEvents.CALL_ENDED, "threadId" to threadId),
            )
        }

        return DefaultResponse(httpStatusCode = HttpStatusCode.OK.value, status = true, message = "Call ended", data = null)
    }
}
