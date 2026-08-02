package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationHiddenThreadRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.MessageCursor
import ke.co.smartroundclinic.scheduling.data.entity.AppointmentEntity
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListConversationThreadsUseCaseTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    private class FakeAppointmentRepository(private val appointments: List<AppointmentEntity>) : AppointmentRepository {
        override suspend fun book(entity: AppointmentEntity) = Resource.Success<AppointmentEntity?>(null)
        override suspend fun getById(id: String) = Resource.Success<AppointmentEntity?>(null)
        override suspend fun getAll() = Resource.Success(appointments)
        override suspend fun getAllForAdmin(status: String?, page: Int, size: Int) = Resource.Success(appointments to appointments.size.toLong())
        override suspend fun getByPatient(patientId: String) = Resource.Success(appointments.filter { it.patientId == patientId })
        override suspend fun getByDoctor(doctorId: String) = Resource.Success(appointments.filter { it.doctorId == doctorId })
        override suspend fun getByDoctorFiltered(doctorId: String, filter: String?, today: String) = Resource.Success(appointments)
        override suspend fun getByDoctorAndDate(doctorId: String, date: String) = Resource.Success(appointments)
        override suspend fun getByDoctorAndDateRange(doctorId: String, from: String, to: String) = Resource.Success(appointments)
        override suspend fun updateStatus(id: String, status: String, cancellationReason: String?, cancelledBy: String?) =
            Resource.Success<AppointmentEntity?>(null)
        override fun watchByDoctorId(doctorId: String): Flow<AppointmentEntity> = emptyFlow()
        override suspend fun existsConfirmedOrCompletedBetween(doctorId: String, patientId: String): Boolean =
            appointments.any { it.doctorId == doctorId && it.patientId == patientId && (it.status == "CONFIRMED" || it.status == "COMPLETED") }
        override suspend fun hasJoinableConfirmedAppointment(doctorId: String, patientId: String): Boolean = false
        override suspend fun getNextConfirmedAppointment(doctorId: String, patientId: String) = Resource.Success<AppointmentEntity?>(null)
        override suspend fun setReferralId(appointmentId: String, referralId: String) = Resource.Success(true)
    }

    private class FakeMessageRepository(
        private val userInfo: Map<String, Pair<String, String?>> = emptyMap(),
        private val latestByPair: Map<Pair<String, String>, ConsultationMessageEntity> = emptyMap(),
    ) : ConsultationMessageRepository {
        override suspend fun save(entity: ConsultationMessageEntity) = Resource.Success(entity)
        override suspend fun getByThread(doctorId: String, patientId: String, before: MessageCursor?, size: Int) =
            Resource.Success(emptyList<ConsultationMessageEntity>())
        override fun watchMessagesForThread(doctorId: String, patientId: String): Flow<ConsultationMessageEntity> = emptyFlow()
        override suspend fun getUserName(userId: String): String? = userInfo[userId]?.first
        override suspend fun getUserInfo(userId: String): Pair<String, String?>? = userInfo[userId]
        override suspend fun getLatestForThread(doctorId: String, patientId: String): ConsultationMessageEntity? =
            latestByPair[doctorId to patientId]
        override suspend fun getLastSeenAt(userId: String): String? = null
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

    private fun appointment(id: String, doctorId: String, patientId: String, bookedAt: String, status: String = "CONFIRMED") =
        AppointmentEntity(
            id = id, doctorId = doctorId, patientId = patientId, date = "2026-01-01",
            slotStart = "09:00", slotEnd = "09:30", status = status, bookedAt = bookedAt,
        )

    private fun textMessage(doctorId: String, patientId: String, text: String, createdAt: String) =
        ConsultationMessageEntity(
            id = "m-$createdAt", doctorId = doctorId, patientId = patientId,
            senderId = doctorId, senderRole = "DOCTOR", senderName = "Dr. Jane",
            messageType = MessageType.TEXT, message = text, createdAt = createdAt,
        )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `doctor role resolves counterpart from patientId`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository(userInfo = mapOf("pat-1" to ("Patient Pat" to "pic.jpg")))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals("Patient Pat", response.data?.first()?.counterpartName)
        assertEquals("pic.jpg", response.data?.first()?.counterpartPicture)
    }

    @Test
    fun `patient role resolves counterpart from doctorId`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository(userInfo = mapOf("doc-1" to ("Dr. Jane" to null)))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("pat-1", "PATIENT")

        assertEquals("Dr. Jane", response.data?.first()?.counterpartName)
        assertNull(response.data?.first()?.counterpartPicture)
    }

    @Test
    fun `falls back to Unknown when counterpart lookup fails`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), FakeMessageRepository(), FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals("Unknown", response.data?.first()?.counterpartName)
    }

    @Test
    fun `only CONFIRMED or COMPLETED appointments produce a thread`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z", status = "PENDING"))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), FakeMessageRepository(), FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(0, response.data?.size)
    }

    @Test
    fun `formats last message preview per message type`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"))
        val fileMessage = ConsultationMessageEntity(
            id = "m1", doctorId = "doc-1", patientId = "pat-1",
            senderId = "doc-1", senderRole = "DOCTOR", senderName = "Dr. Jane",
            messageType = MessageType.FILE, files = listOf(ConsultationFile("xray.png", "key", "image/png", 10L)),
            createdAt = "2026-01-01T00:00:00.000Z",
        )
        val messages = FakeMessageRepository(latestByPair = mapOf(("doc-1" to "pat-1") to fileMessage))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals("xray.png", response.data?.first()?.lastMessagePreview)
    }

    @Test
    fun `sorts threads by lastMessageAt descending`(): Unit = runBlocking {
        val appointments = listOf(
            appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"),
            appointment("appt-2", "doc-1", "pat-2", "2026-01-01T00:00:00.000Z"),
        )
        val messages = FakeMessageRepository(
            latestByPair = mapOf(
                ("doc-1" to "pat-1") to textMessage("doc-1", "pat-1", "older", "2026-01-01T00:00:00.000Z"),
                ("doc-1" to "pat-2") to textMessage("doc-1", "pat-2", "newer", "2026-02-01T00:00:00.000Z"),
            )
        )
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(listOf("pat-2", "pat-1"), response.data?.map { it.patientId })
    }

    // ── Hidden-thread ("delete for me") filter — table test ─────────────────────

    @Test
    fun `never-hidden thread is visible`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository(latestByPair = mapOf(("doc-1" to "pat-1") to textMessage("doc-1", "pat-1", "hi", "2026-01-01T00:00:00.000Z")))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), messages, FakeHiddenThreadRepository(), FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(1, response.data?.size)
    }

    @Test
    fun `hidden thread with no messages since hiding stays hidden`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository(latestByPair = mapOf(("doc-1" to "pat-1") to textMessage("doc-1", "pat-1", "hi", "2026-01-01T00:00:00.000Z")))
        val hidden = FakeHiddenThreadRepository(hidden = mapOf(("doc-1" to "pat-1") to "2026-01-02T00:00:00.000Z"))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), messages, hidden, FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(0, response.data?.size)
    }

    @Test
    fun `hidden thread with a newer message reappears`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository(latestByPair = mapOf(("doc-1" to "pat-1") to textMessage("doc-1", "pat-1", "new one", "2026-01-03T00:00:00.000Z")))
        val hidden = FakeHiddenThreadRepository(hidden = mapOf(("doc-1" to "pat-1") to "2026-01-02T00:00:00.000Z"))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), messages, hidden, FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(1, response.data?.size)
    }

    @Test
    fun `hidden thread with no messages at all stays hidden`(): Unit = runBlocking {
        val appointments = listOf(appointment("appt-1", "doc-1", "pat-1", "2026-01-01T00:00:00.000Z"))
        val messages = FakeMessageRepository() // no latest message for this pair — lastMessageAt is null
        val hidden = FakeHiddenThreadRepository(hidden = mapOf(("doc-1" to "pat-1") to "2026-01-02T00:00:00.000Z"))
        val useCase = ListConversationThreadsUseCase(FakeAppointmentRepository(appointments), messages, hidden, FakeRedisRepository())

        val response = useCase("doc-1", "DOCTOR")

        assertEquals(0, response.data?.size)
    }
}
