package ke.co.smartroundclinic.consultation.domain.usecase.call

import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadRepository
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
    private val threads: ConsultationThreadRepository,
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

        when (val r = threads.getByVideoRoomId(meetingId)) {
            is Resource.Success -> {
                val thread = r.data
                if (thread != null) {
                    threads.clearVideoRoomId(thread.doctorId, thread.patientId, completedRoomId = meetingId)
                    log.info("Webhook: cleared videoRoomId for doctorId=${thread.doctorId} patientId=${thread.patientId}")
                    runCatching {
                        notificationSender?.send(
                            title = PushNotificationEvents.CALL_ENDED,
                            message = "The video call has ended",
                            channel = NotificationChannel.PUSH_NOTIFICATION,
                            destination = NotificationDestination.DOCTOR,
                            recipientId = thread.doctorId,
                            metadata = mapOf(
                                "event" to PushNotificationEvents.CALL_ENDED,
                                "doctorId" to thread.doctorId,
                                "patientId" to thread.patientId,
                            ),
                        )
                        notificationSender?.send(
                            title = PushNotificationEvents.CALL_ENDED,
                            message = "The video call has ended",
                            channel = NotificationChannel.PUSH_NOTIFICATION,
                            destination = NotificationDestination.PATIENT,
                            recipientId = thread.patientId,
                            metadata = mapOf(
                                "event" to PushNotificationEvents.CALL_ENDED,
                                "doctorId" to thread.doctorId,
                                "patientId" to thread.patientId,
                            ),
                        )
                    }
                } else {
                    log.info("Webhook: no thread found for meetingId=$meetingId — already cleaned up")
                }
            }
            is Resource.Error -> log.error("Webhook: failed to look up thread for meetingId=$meetingId — ${r.message}")
        }
    }
}
