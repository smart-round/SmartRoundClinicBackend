package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.ConsultationSessionEntity
import ke.co.smartroundclinic.consultation.data.entity.ConsultationStatus
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.domain.repository.MessageCursor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListConversationThreadsUseCaseTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    private class FakeSessionRepository(private val threads: List<ConsultationSessionEntity>) : ConsultationSessionRepository {
        override suspend fun startOrGet(appointmentId: String, userId: String) = Resource.Error<ConsultationSessionEntity>("not used")
        override suspend fun listThreadsForUser(userId: String, role: String) = Resource.Success(threads)
        override suspend fun getAllSessions() = Resource.Success(threads)
        override suspend fun getById(id: String) = Resource.Success<ConsultationSessionEntity?>(null)
        override suspend fun getByAppointmentId(appointmentId: String) = Resource.Success<ConsultationSessionEntity?>(null)
        override suspend fun getByVideoRoomId(videoRoomId: String) = Resource.Success<ConsultationSessionEntity?>(null)
        override suspend fun setVideoRoomId(id: String, videoRoomId: String) = Resource.Success<ConsultationSessionEntity?>(null)
        override suspend fun setVideoRoomIdIfAbsent(id: String, videoRoomId: String) = Resource.Success(videoRoomId)
        override suspend fun clearVideoRoomId(id: String, completedRoomId: String?) = Resource.Success(Unit)
        override suspend fun end(id: String, doctorId: String) = Resource.Success<ConsultationSessionEntity?>(null)
    }

    private class FakeMessageRepository(
        private val userInfo: Map<String, Pair<String, String?>> = emptyMap(),
        private val latestByPair: Map<Pair<String, String>, ConsultationMessageEntity> = emptyMap(),
    ) : ConsultationMessageRepository {
        override suspend fun save(entity: ConsultationMessageEntity) = Resource.Success(entity)
        override suspend fun getByConsultationId(consultationId: String, page: Int, size: Int) =
            Resource.Success(emptyList<ConsultationMessageEntity>() to 0L)
        override suspend fun getByThread(doctorId: String, patientId: String, before: MessageCursor?, size: Int) =
            Resource.Success(emptyList<ConsultationMessageEntity>())
        override fun watchMessages(consultationId: String): Flow<ConsultationMessageEntity> = emptyFlow()
        override suspend fun getUserName(userId: String): String? = userInfo[userId]?.first
        override suspend fun getUserInfo(userId: String): Pair<String, String?>? = userInfo[userId]
        override suspend fun getLatestForThread(doctorId: String, patientId: String): ConsultationMessageEntity? =
            latestByPair[doctorId to patientId]
        override suspend fun getLastSeenAt(userId: String): String? = null
        override suspend fun countMissingThreadFields(): Long = 0L
        override suspend fun backfillThreadFields(consultationId: String, doctorId: String, patientId: String): Long = 0L
    }

    private class FakeHiddenThreadRepository(
        private val hidden: Map<Pair<String, String>, String> = emptyMap(),
    ) : ConsultationHiddenThreadRepository {
        override suspend fun hide(userId: String, doctorId: String, patientId: String) = Resource.Success(Unit)
        override suspend fun getHiddenMap(userId: String) = Resource.Success(hidden)
    }

    /** Always reports offline — presence/last-seen aren't under test here except where noted. */
    private class FakeRedisRepository : RedisRepository {
        override suspend fun set(key: String, value: String, ttlSeconds: Long?) {}
        override suspend fun get(key: String): String? = null
        override suspend fun delete(key: String) {}
        override suspend fun exists(key: String): Boolean = false
        override suspend fun expire(key: String, ttlSeconds: Long) {}
        override suspend fun setIfAbsent(key: String, value: String, ttlSeconds: Long?): Boolean = true
        override suspend fun increment(key: String): Long = 0L
        override suspend fun getAndDelete(key: String): String? = null
    }

    private fun session(id: String, doctorId: String, patientId: String, appointmentId: String, createdAt: String, status: ConsultationStatus = ConsultationStatus.ACTIVE) =
        ConsultationSessionEntity(id = id, appointmentId = appointmentId, doctorId = doctorId, patientId = patientId, status = status, createdAt = createdAt)

    private fun textMessage(doctorId: String, patientId: String, text: String, createdAt: String) =
        ConsultationMessageEntity(
            id = "m-$createdAt", consultationId = "c1", doctorId = doctorId, patientId = patientId,
            senderId = doctorId, senderRole = "DOCTOR", senderName = "Dr. Jane",
            messageType = MessageType.TEXT, message = text, createdAt = createdAt,
        )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `doctor role resolves counterpart from patientId`(): Unit = runBlocking {
        val threads = listOf(session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"))
        val sessions = FakeSessionRepository(threads)
        val messages = FakeMessageRepository(userInfo = mapOf("pat-1" to ("Patient Pat" to "pic.jpg")))
        val useCase = ListConversationThreadsUseCase(sessions, messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals("Patient Pat", response.data?.first()?.counterpartName)
        assertEquals("pic.jpg", response.data?.first()?.counterpartPicture)
    }

    @Test
    fun `patient role resolves counterpart from doctorId`(): Unit = runBlocking {
        val threads = listOf(session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"))
        val sessions = FakeSessionRepository(threads)
        val messages = FakeMessageRepository(userInfo = mapOf("doc-1" to ("Dr. Jane" to null)))
        val useCase = ListConversationThreadsUseCase(sessions, messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("pat-1", "PATIENT")

        assertEquals("Dr. Jane", response.data?.first()?.counterpartName)
        assertNull(response.data?.first()?.counterpartPicture)
    }

    @Test
    fun `falls back to Unknown when counterpart lookup fails`(): Unit = runBlocking {
        val threads = listOf(session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"))
        val useCase = ListConversationThreadsUseCase(FakeSessionRepository(threads), FakeMessageRepository(), FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals("Unknown", response.data?.first()?.counterpartName)
    }

    @Test
    fun `formats last message preview per message type`(): Unit = runBlocking {
        val threads = listOf(session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"))
        val fileMessage = ConsultationMessageEntity(
            id = "m1", consultationId = "c1", doctorId = "doc-1", patientId = "pat-1",
            senderId = "doc-1", senderRole = "DOCTOR", senderName = "Dr. Jane",
            messageType = MessageType.FILE, files = listOf(ConsultationFile("xray.png", "key", "image/png", 10L)),
            createdAt = "2026-01-01T00:00:00.000Z",
        )
        val messages = FakeMessageRepository(latestByPair = mapOf(("doc-1" to "pat-1") to fileMessage))
        val useCase = ListConversationThreadsUseCase(FakeSessionRepository(threads), messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals("xray.png", response.data?.first()?.lastMessagePreview)
    }

    @Test
    fun `sorts threads by lastMessageAt descending`(): Unit = runBlocking {
        val threads = listOf(
            session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"),
            session("s2", "doc-1", "pat-2", "appt-2", "2026-01-01T00:00:00.000Z"),
        )
        val messages = FakeMessageRepository(
            latestByPair = mapOf(
                ("doc-1" to "pat-1") to textMessage("doc-1", "pat-1", "older", "2026-01-01T00:00:00.000Z"),
                ("doc-1" to "pat-2") to textMessage("doc-1", "pat-2", "newer", "2026-02-01T00:00:00.000Z"),
            )
        )
        val useCase = ListConversationThreadsUseCase(FakeSessionRepository(threads), messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(listOf("pat-2", "pat-1"), response.data?.map { it.patientId })
    }

    // ── Hidden-thread ("delete for me") filter — table test ─────────────────────

    @Test
    fun `never-hidden thread is visible`(): Unit = runBlocking {
        val threads = listOf(session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository(latestByPair = mapOf(("doc-1" to "pat-1") to textMessage("doc-1", "pat-1", "hi", "2026-01-01T00:00:00.000Z")))
        val useCase = ListConversationThreadsUseCase(FakeSessionRepository(threads), messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(1, response.data?.size)
    }

    @Test
    fun `hidden thread with no messages since hiding stays hidden`(): Unit = runBlocking {
        val threads = listOf(session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository(latestByPair = mapOf(("doc-1" to "pat-1") to textMessage("doc-1", "pat-1", "hi", "2026-01-01T00:00:00.000Z")))
        val hidden = FakeHiddenThreadRepository(hidden = mapOf(("doc-1" to "pat-1") to "2026-01-02T00:00:00.000Z"))
        val useCase = ListConversationThreadsUseCase(FakeSessionRepository(threads), messages, hidden, FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(0, response.data?.size)
    }

    @Test
    fun `hidden thread with a newer message reappears`(): Unit = runBlocking {
        val threads = listOf(session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository(latestByPair = mapOf(("doc-1" to "pat-1") to textMessage("doc-1", "pat-1", "new one", "2026-01-03T00:00:00.000Z")))
        val hidden = FakeHiddenThreadRepository(hidden = mapOf(("doc-1" to "pat-1") to "2026-01-02T00:00:00.000Z"))
        val useCase = ListConversationThreadsUseCase(FakeSessionRepository(threads), messages, hidden, FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(1, response.data?.size)
    }

    @Test
    fun `hidden thread with no messages at all stays hidden`(): Unit = runBlocking {
        val threads = listOf(session("s1", "doc-1", "pat-1", "appt-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository() // no latest message for this pair — lastMessageAt is null
        val hidden = FakeHiddenThreadRepository(hidden = mapOf(("doc-1" to "pat-1") to "2026-01-02T00:00:00.000Z"))
        val useCase = ListConversationThreadsUseCase(FakeSessionRepository(threads), messages, hidden, FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(0, response.data?.size)
    }
}
