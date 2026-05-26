package ke.co.smartroundclinic.consultation.domain.usecase.call

import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import org.slf4j.LoggerFactory

/**
 * Triggered by a Cloudflare RealtimeKit webhook.
 *
 * - [forceCleanup] = true  → `meeting.ended` event: meeting is definitively over,
 *                            skip the REST status check and clean up immediately.
 * - [forceCleanup] = false → `meeting.participantLeft` event: verify the meeting
 *                            is now INACTIVE via the REST API before cleaning up
 *                            (another participant may still be connected).
 */
class HandleMeetingEndedWebhookUseCase(
    private val sessions: ConsultationSessionRepository,
    private val client: RealtimeKitClient,
    private val notificationSender: NotificationSender? = null,
) {
    private val log = LoggerFactory.getLogger(HandleMeetingEndedWebhookUseCase::class.java)

    suspend operator fun invoke(meetingId: String, forceCleanup: Boolean) {
        if (!forceCleanup) {
            val status = client.getMeetingStatus(meetingId)
            if (status == null || status != "INACTIVE") {
                log.info("Webhook: meeting=$meetingId status=$status — skipping cleanup (participants still connected)")
                return
            }
        }

        log.info("Webhook: cleaning up meeting=$meetingId (force=$forceCleanup)")

        client.endMeeting(meetingId)

        when (val r = sessions.getByVideoRoomId(meetingId)) {
            is Resource.Success -> {
                val session = r.data
                if (session != null) {
                    sessions.clearVideoRoomId(session.id)
                    log.info("Webhook: cleared videoRoomId on consultation=${session.id}")
                    runCatching {
                        notificationSender?.send(
                            title = "Call Ended",
                            message = "The video call has ended",
                            channel = NotificationChannel.PUSH_NOTIFICATION,
                            destination = NotificationDestination.DOCTOR,
                            recipientId = session.doctorId,
                        )
                        notificationSender?.send(
                            title = "Call Ended",
                            message = "The video call has ended",
                            channel = NotificationChannel.PUSH_NOTIFICATION,
                            destination = NotificationDestination.PATIENT,
                            recipientId = session.patientId,
                        )
                    }
                } else {
                    log.info("Webhook: no consultation found for meetingId=$meetingId — already cleaned up")
                }
            }
            is Resource.Error -> log.error("Webhook: failed to look up consultation for meetingId=$meetingId — ${r.message}")
        }
    }
}
