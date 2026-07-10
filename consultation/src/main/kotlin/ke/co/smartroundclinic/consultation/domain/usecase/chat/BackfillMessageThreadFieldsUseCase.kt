package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.infra.plugins.BackgroundTask
import org.slf4j.LoggerFactory

// Idempotent — execute() short-circuits once every message already has doctorId/patientId, so a long
// interval just means "check again occasionally", not "re-run the backfill on a schedule".
private const val BACKFILL_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

/**
 * One-off backfill: populates doctorId/patientId on [ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity]
 * documents written before those fields existed, by cross-referencing their consultation session.
 * Runs once at startup (BackgroundTask.execute fires immediately) and is safe to re-run — it only
 * touches messages that still have the field missing.
 */
class BackfillMessageThreadFieldsUseCase(
    private val sessionRepository: ConsultationSessionRepository,
    private val messageRepository: ConsultationMessageRepository,
) : BackgroundTask {
    private val log = LoggerFactory.getLogger(BackfillMessageThreadFieldsUseCase::class.java)

    override val name = "backfill-consultation-message-thread-fields"
    override val intervalMs = BACKFILL_CHECK_INTERVAL_MS

    override suspend fun execute() {
        val pending = messageRepository.countMissingThreadFields()
        if (pending == 0L) return

        log.info("[Backfill] $pending consultation message(s) missing doctorId/patientId — backfilling from sessions")
        val sessions = when (val result = sessionRepository.getAllSessions()) {
            is Resource.Success -> result.data ?: emptyList()
            is Resource.Error -> {
                log.warn("[Backfill] Failed to load consultation sessions — ${result.message}")
                return
            }
        }

        var updated = 0L
        sessions.forEach { session ->
            updated += messageRepository.backfillThreadFields(session.id, session.doctorId, session.patientId)
        }
        log.info("[Backfill] Updated $updated message(s) with doctorId/patientId")
    }
}
