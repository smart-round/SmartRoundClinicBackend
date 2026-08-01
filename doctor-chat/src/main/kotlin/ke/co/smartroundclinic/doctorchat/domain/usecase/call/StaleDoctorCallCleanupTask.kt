package ke.co.smartroundclinic.doctorchat.domain.usecase.call

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import ke.co.smartroundclinic.infra.plugins.BackgroundTask
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private const val STALE_THRESHOLD_HOURS = 1
private const val INTERVAL_MINUTES = 10L

/**
 * Doctor-chat's own copy of [ke.co.smartroundclinic.consultation.domain.usecase.call.StaleCallCleanupTask].
 * Both tasks call `listActiveMeetings()` independently and each only recognizes its own kind of
 * thread (the other's `getByVideoRoomId` lookup just misses and logs, harmlessly) — a small
 * redundancy accepted in exchange for zero shared code/state with the consultation cleanup path.
 *
 * This is also this feature's only cleanup path for a call nobody explicitly ended (no webhook
 * integration this round — see the doctor-chat module's design notes): worst case, an abandoned
 * call stays "active" for up to ~1h10m before this task ends it.
 */
class StaleDoctorCallCleanupTask(
    private val client: RealtimeKitClient,
    private val threads: DoctorChatThreadRepository,
) : BackgroundTask {

    private val log = LoggerFactory.getLogger(StaleDoctorCallCleanupTask::class.java)

    override val name = "stale-doctor-call-cleanup"
    override val intervalMs = INTERVAL_MINUTES * 60 * 1000L

    override suspend fun execute() {
        val activeMeetings = client.listActiveMeetings()
        if (activeMeetings.isEmpty()) return

        val threshold = Clock.System.now().minus(STALE_THRESHOLD_HOURS.hours)
        val staleMeetings = activeMeetings.filter { meeting ->
            val createdAt = meeting.createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
            createdAt != null && createdAt < threshold && meeting.title?.contains("doctors-lounge", ignoreCase = true) == true
        }
        if (staleMeetings.isEmpty()) return

        log.info("[StaleDoctorCallCleanup] Found ${staleMeetings.size} stale doctor-chat meeting(s) older than ${STALE_THRESHOLD_HOURS}h")

        for (meeting in staleMeetings) {
            log.info("[StaleDoctorCallCleanup] Ending stale meeting=${meeting.id} createdAt=${meeting.createdAt}")
            when (val endResult = client.endMeeting(meeting.id)) {
                is Resource.Error -> {
                    log.warn("[StaleDoctorCallCleanup] Failed to end meeting=${meeting.id} — ${endResult.message}")
                    continue
                }
                else -> Unit
            }
            when (val lookup = threads.getByVideoRoomId(meeting.id)) {
                is Resource.Success -> {
                    val thread = lookup.data
                    if (thread != null) {
                        threads.clearVideoRoomId(thread.id, completedRoomId = meeting.id)
                        log.info("[StaleDoctorCallCleanup] Cleared videoRoomId for threadId=${thread.id}")
                    } else {
                        log.info("[StaleDoctorCallCleanup] No thread found for meeting=${meeting.id}")
                    }
                }
                is Resource.Error -> log.warn("[StaleDoctorCallCleanup] Thread lookup failed for meeting=${meeting.id} — ${lookup.message}")
            }
        }
    }
}
