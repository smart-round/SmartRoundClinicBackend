package ke.co.smartroundclinic.consultation.domain.usecase.call

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import org.slf4j.LoggerFactory

/**
 * Triggered by a Cloudflare RealtimeKit webhook when meeting participants change.
 *
 * If the meeting is now INACTIVE (all participants have left), this use case:
 *   1. Calls Cloudflare DELETE /meetings/{id} to ensure the meeting is fully ended
 *   2. Clears the videoRoomId on the consultation session so the next join
 *      provisions a fresh meeting room
 *
 * Configure the webhook URL in the Cloudflare dashboard:
 *   POST https://<your-domain>/webhooks/cloudflare/realtime-kit
 */
class HandleMeetingEndedWebhookUseCase(
    private val sessions: ConsultationSessionRepository,
    private val client: RealtimeKitClient,
) {
    private val log = LoggerFactory.getLogger(HandleMeetingEndedWebhookUseCase::class.java)

    suspend operator fun invoke(meetingId: String) {
        val status = client.getMeetingStatus(meetingId)
        if (status == null || status != "INACTIVE") {
            log.info("Webhook: meeting=$meetingId status=$status — skipping cleanup (meeting still active)")
            return
        }

        log.info("Webhook: meeting=$meetingId is INACTIVE — cleaning up")

        // End the meeting on Cloudflare (idempotent — safe even if already INACTIVE)
        client.endMeeting(meetingId)

        // Clear the stored meeting ID so the next join creates a fresh room
        when (val r = sessions.getByVideoRoomId(meetingId)) {
            is Resource.Success -> {
                val session = r.data
                if (session != null) {
                    sessions.clearVideoRoomId(session.id)
                    log.info("Webhook: cleared videoRoomId on consultation=${session.id}")
                } else {
                    log.warn("Webhook: no consultation found for meetingId=$meetingId")
                }
            }
            is Resource.Error -> log.error("Webhook: failed to look up consultation for meetingId=$meetingId — ${r.message}")
        }
    }
}
