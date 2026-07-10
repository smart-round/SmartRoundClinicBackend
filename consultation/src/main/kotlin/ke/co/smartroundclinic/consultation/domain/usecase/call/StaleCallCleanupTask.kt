package ke.co.smartroundclinic.consultation.domain.usecase.call

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadRepository
import ke.co.smartroundclinic.infra.plugins.BackgroundTask
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private const val STALE_THRESHOLD_HOURS = 1
private const val INTERVAL_MINUTES = 10L

class StaleCallCleanupTask(
    private val client: RealtimeKitClient,
    private val threads: ConsultationThreadRepository,
) : BackgroundTask {

    private val log = LoggerFactory.getLogger(StaleCallCleanupTask::class.java)

    override val name = "stale-call-cleanup"
    override val intervalMs = INTERVAL_MINUTES * 60 * 1000L

    override suspend fun execute() {
        val activeMeetings = client.listActiveMeetings()
        if (activeMeetings.isEmpty()) return

        val threshold = Clock.System.now().minus(STALE_THRESHOLD_HOURS.hours)

        val staleMeetings = activeMeetings.filter { meeting ->
            val createdAt = meeting.createdAt
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            createdAt != null && createdAt < threshold
        }

        if (staleMeetings.isEmpty()) return

        log.info("[StaleCallCleanup] Found ${staleMeetings.size} stale meeting(s) older than ${STALE_THRESHOLD_HOURS}h")

        for (meeting in staleMeetings) {
            log.info("[StaleCallCleanup] Ending stale meeting=${meeting.id} createdAt=${meeting.createdAt}")

            when (val endResult = client.endMeeting(meeting.id)) {
                is Resource.Error -> {
                    log.warn("[StaleCallCleanup] Failed to end meeting=${meeting.id} — ${endResult.message}")
                    continue
                }
                else -> Unit
            }

            when (val lookup = threads.getByVideoRoomId(meeting.id)) {
                is Resource.Success -> {
                    val thread = lookup.data
                    if (thread != null) {
                        threads.clearVideoRoomId(thread.doctorId, thread.patientId, completedRoomId = meeting.id)
                        log.info("[StaleCallCleanup] Cleared videoRoomId for doctorId=${thread.doctorId} patientId=${thread.patientId}")
                    } else {
                        log.info("[StaleCallCleanup] No thread found for meeting=${meeting.id}")
                    }
                }
                is Resource.Error -> log.warn("[StaleCallCleanup] Thread lookup failed for meeting=${meeting.id} — ${lookup.message}")
            }
        }
    }
}
