package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.ConsultationSessionEntity
import ke.co.smartroundclinic.consultation.data.entity.ConsultationStatus
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.domain.repository.MessageCursor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackfillMessageThreadFieldsUseCaseTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    private class FakeSessionRepository(private val sessions: List<ConsultationSessionEntity>) : ConsultationSessionRepository {
        var getAllSessionsCallCount = 0
            private set

        override suspend fun startOrGet(appointmentId: String, userId: String) = Resource.Error<ConsultationSessionEntity>("not used")
        override suspend fun listThreadsForUser(userId: String, role: String) = Resource.Success(sessions)
        override suspend fun getAllSessions(): Resource<List<ConsultationSessionEntity>> {
            getAllSessionsCallCount++
            return Resource.Success(sessions)
        }
        override suspend fun getById(id: String) = Resource.Success<ConsultationSessionEntity?>(null)
        override suspend fun getByAppointmentId(appointmentId: String) = Resource.Success<ConsultationSessionEntity?>(null)
        override suspend fun getByVideoRoomId(videoRoomId: String) = Resource.Success<ConsultationSessionEntity?>(null)
        override suspend fun setVideoRoomId(id: String, videoRoomId: String) = Resource.Success<ConsultationSessionEntity?>(null)
        override suspend fun setVideoRoomIdIfAbsent(id: String, videoRoomId: String) = Resource.Success(videoRoomId)
        override suspend fun clearVideoRoomId(id: String, completedRoomId: String?) = Resource.Success(Unit)
        override suspend fun end(id: String, doctorId: String) = Resource.Success<ConsultationSessionEntity?>(null)
    }

    /** Each backfill call simulates catching up 2 messages for that session, draining [pending] until 0. */
    private class FakeMessageRepository(initialPending: Long) : ConsultationMessageRepository {
        var pending = initialPending
            private set
        val backfillCalls = mutableListOf<Triple<String, String, String>>()

        override suspend fun save(entity: ConsultationMessageEntity) = Resource.Success(entity)
        override suspend fun getByConsultationId(consultationId: String, page: Int, size: Int) =
            Resource.Success(emptyList<ConsultationMessageEntity>() to 0L)
        override suspend fun getByThread(doctorId: String, patientId: String, before: MessageCursor?, size: Int) =
            Resource.Success(emptyList<ConsultationMessageEntity>())
        override fun watchMessages(consultationId: String): Flow<ConsultationMessageEntity> = emptyFlow()
        override suspend fun getUserName(userId: String): String? = null
        override suspend fun getUserInfo(userId: String): Pair<String, String?>? = null
        override suspend fun getLatestForThread(doctorId: String, patientId: String): ConsultationMessageEntity? = null
        override suspend fun getLastSeenAt(userId: String): String? = null
        override suspend fun countMissingThreadFields(): Long = pending
        override suspend fun backfillThreadFields(consultationId: String, doctorId: String, patientId: String): Long {
            backfillCalls += Triple(consultationId, doctorId, patientId)
            val updated = minOf(2L, pending)
            pending -= updated
            return updated
        }
    }

    private fun session(id: String, doctorId: String, patientId: String) = ConsultationSessionEntity(
        id = id, appointmentId = "appt-$id", doctorId = doctorId, patientId = patientId, status = ConsultationStatus.ENDED,
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `does nothing when no messages are missing thread fields`(): Unit = runBlocking {
        val sessions = FakeSessionRepository(listOf(session("s1", "doc-1", "pat-1")))
        val messages = FakeMessageRepository(initialPending = 0L)
        val useCase = BackfillMessageThreadFieldsUseCase(sessions, messages)

        useCase.execute()

        assertEquals(0, sessions.getAllSessionsCallCount, "should short-circuit before loading sessions")
        assertTrue(messages.backfillCalls.isEmpty())
    }

    @Test
    fun `backfills every session from doctorId and patientId when messages are missing`(): Unit = runBlocking {
        val sessions = FakeSessionRepository(listOf(
            session("s1", "doc-1", "pat-1"),
            session("s2", "doc-1", "pat-2"),
            session("s3", "doc-2", "pat-1"),
        ))
        val messages = FakeMessageRepository(initialPending = 6L)
        val useCase = BackfillMessageThreadFieldsUseCase(sessions, messages)

        useCase.execute()

        assertEquals(3, messages.backfillCalls.size)
        assertEquals(
            setOf(Triple("s1", "doc-1", "pat-1"), Triple("s2", "doc-1", "pat-2"), Triple("s3", "doc-2", "pat-1")),
            messages.backfillCalls.toSet(),
        )
        assertEquals(0L, messages.pending)
    }

    @Test
    fun `second run is a no-op once fully backfilled`(): Unit = runBlocking {
        val sessions = FakeSessionRepository(listOf(session("s1", "doc-1", "pat-1")))
        val messages = FakeMessageRepository(initialPending = 2L)
        val useCase = BackfillMessageThreadFieldsUseCase(sessions, messages)

        useCase.execute()
        assertEquals(1, messages.backfillCalls.size)
        assertEquals(0L, messages.pending)

        useCase.execute() // idempotent re-run

        assertEquals(1, messages.backfillCalls.size, "no new backfill calls once pending is 0")
        assertEquals(1, sessions.getAllSessionsCallCount, "sessions should not be reloaded on the no-op run")
    }
}
