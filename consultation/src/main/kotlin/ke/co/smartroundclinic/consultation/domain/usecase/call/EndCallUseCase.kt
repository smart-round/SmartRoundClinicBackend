package ke.co.smartroundclinic.consultation.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient

/**
 * Doctor ends the active video call:
 *   1. Verifies the caller is the doctor on the session
 *   2. Ends the Cloudflare meeting (kicks all participants)
 *   3. Clears `videoRoomId` on the session so the next join provisions a fresh meeting
 */
class EndCallUseCase(
    private val sessions: ConsultationSessionRepository,
    private val client: RealtimeKitClient,
    private val notificationSender: NotificationSender? = null,
) {
    suspend operator fun invoke(consultationId: String, doctorId: String): DefaultResponse<Unit?> {
        val session = when (val r = sessions.getById(consultationId)) {
            is Resource.Success -> r.data
            is Resource.Error -> return DefaultResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                status = false,
                message = r.message ?: "Failed to load consultation",
                data = null,
            )
        } ?: return DefaultResponse(
            httpStatusCode = HttpStatusCode.NotFound.value,
            status = false,
            message = "Consultation not found",
            data = null,
        )

        if (doctorId != session.doctorId) return DefaultResponse(
            httpStatusCode = HttpStatusCode.Forbidden.value,
            status = false,
            message = "Only the doctor can end the call",
            data = null,
        )

        val meetingId = session.videoRoomId
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
            sessions.clearVideoRoomId(consultationId)
        }

        runCatching {
            notificationSender?.send(
                title = "Call Ended",
                message = "The video call has been ended by your doctor",
                channel = NotificationChannel.PUSH_NOTIFICATION,
                destination = NotificationDestination.PATIENT,
                recipientId = session.patientId,
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
