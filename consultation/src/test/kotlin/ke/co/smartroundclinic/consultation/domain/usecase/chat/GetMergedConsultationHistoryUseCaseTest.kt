package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationFile
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.data.entity.ConsultationThreadReadStateEntity
import ke.co.smartroundclinic.consultation.data.entity.MessageType
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadReadStateRepository
import ke.co.smartroundclinic.consultation.domain.repository.MessageCursor
import ke.co.smartroundclinic.infra.storage.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * These tests exercise the merged-history use case against an in-memory fake that mirrors the
 * intended semantics of [ConsultationMessageRepositoryImpl.getByThread] (filter by doctorId+patientId,
 * sort by createdAt/id descending, cursor via `before`). They verify the use case's cursor
 * encode/decode round-trip and pagination logic. They do NOT verify the real MongoDB query
 * (Filters/Sorts construction) in the Impl — this repo has no MongoDB test infrastructure
 * (testcontainers/embedded Mongo), so that layer needs manual/staging verification.
 */
class GetMergedConsultationHistoryUseCaseTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    /** In-memory stand-in for the Mongo-backed repository — same filter/sort/cursor contract as the real impl. */
    private class FakeThreadMessageRepository(all: List<ConsultationMessageEntity>) : ConsultationMessageRepository {
        private val store = all.toMutableList()

        override suspend fun save(entity: ConsultationMessageEntity): Resource<ConsultationMessageEntity> {
            store += entity
            return Resource.Success(entity)
        }

        override suspend fun getByThread(
            doctorId: String,
            patientId: String,
            before: MessageCursor?,
            size: Int,
        ): Resource<List<ConsultationMessageEntity>> {
            val matching = store
                .filter { it.doctorId == doctorId && it.patientId == patientId }
                .sortedWith(compareByDescending<ConsultationMessageEntity> { it.createdAt }.thenByDescending { it.id })
                .let { sorted ->
                    if (before == null) sorted
                    else sorted.filter { it.createdAt < before.createdAt || (it.createdAt == before.createdAt && it.id < before.id) }
                }
                .take(size)
            return Resource.Success(matching)
        }

        override fun watchMessagesForThread(doctorId: String, patientId: String): Flow<ConsultationMessageEntity> = emptyFlow()
        override suspend fun getUserName(userId: String): String? = null
        override suspend fun getUserInfo(userId: String): Pair<String, String?>? = null
        override suspend fun getLatestForThread(doctorId: String, patientId: String): ConsultationMessageEntity? = null
        override suspend fun getLastSeenAt(userId: String): String? = null
    }

    private class FakeStorageRepository(private val presignedUrl: String = "https://cdn.test/resolved") : StorageRepository {
        override suspend fun upload(bucket: String, key: String, content: ByteArray, contentType: String) =
            Resource.Success(key)
        override suspend fun delete(bucket: String, key: String) = Resource.Success<Nothing>(null)
        override suspend fun presignedGetUrl(bucket: String, key: String, expiresInSeconds: Long) =
            Resource.Success(presignedUrl)
    }

    private class FakeReadStateRepository(private val state: ConsultationThreadReadStateEntity? = null) : ConsultationThreadReadStateRepository {
        override suspend fun getByPair(doctorId: String, patientId: String): Resource<ConsultationThreadReadStateEntity?> = Resource.Success(state)
        override suspend fun getByPairs(pairs: List<Pair<String, String>>) = Resource.Success(listOfNotNull(state))
        override suspend fun markRead(doctorId: String, patientId: String, readerId: String, at: String): Resource<ConsultationThreadReadStateEntity?> = Resource.Success(state)
        override suspend fun bumpDelivered(doctorId: String, patientId: String, recipientId: String, at: String): Resource<ConsultationThreadReadStateEntity?> = Resource.Success(state)
    }

    private fun message(id: String, createdAt: String, files: List<ConsultationFile> = emptyList()) =
        ConsultationMessageEntity(
            id = id,
            doctorId = "doc-1",
            patientId = "pat-1",
            senderId = "doc-1",
            senderRole = "DOCTOR",
            senderName = "Dr. Jane",
            messageType = if (files.isEmpty()) MessageType.TEXT else MessageType.FILE,
            message = if (files.isEmpty()) "msg-$id" else null,
            files = files,
            createdAt = createdAt,
        )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `merges messages in strict createdAt order`(): Unit = runBlocking {
        val messages = listOf(
            message("1", "2026-01-01T00:00:00.000Z"),
            message("2", "2026-01-01T00:01:00.000Z"),
            message("3", "2026-01-01T00:02:00.000Z"),
            message("4", "2026-01-01T00:03:00.000Z"),
            message("5", "2026-01-01T00:04:00.000Z"),
            message("6", "2026-01-01T00:05:00.000Z"),
        )
        val useCase = GetMergedConsultationHistoryUseCase(FakeThreadMessageRepository(messages), FakeStorageRepository(), FakeReadStateRepository())

        val response = useCase("doc-1", "pat-1", before = null, size = 10, requesterId = "doc-1")

        val ids = response.data?.items?.map { it.id }
        assertEquals(listOf("6", "5", "4", "3", "2", "1"), ids, "expected strict createdAt-descending order")
    }

    @Test
    fun `cursor pagination in size-2 pages returns the same set and order as one unpaged fetch`(): Unit = runBlocking {
        val messages = (1..6).map { i ->
            message(i.toString(), "2026-01-01T00:0$i:00.000Z")
        }
        val repo = FakeThreadMessageRepository(messages)
        val useCase = GetMergedConsultationHistoryUseCase(repo, FakeStorageRepository(), FakeReadStateRepository())

        val unpaged = useCase("doc-1", "pat-1", before = null, size = 6, requesterId = "doc-1").data?.items?.map { it.id } ?: emptyList()

        val paged = mutableListOf<String>()
        var cursor: String? = null
        do {
            val page = useCase("doc-1", "pat-1", before = cursor, size = 2, requesterId = "doc-1")
            paged += page.data?.items?.map { it.id } ?: emptyList()
            cursor = page.data?.nextCursor
        } while (cursor != null)

        assertEquals(unpaged, paged, "paged fetch must exactly match the unpaged fetch, no skips or duplicates")
    }

    @Test
    fun `nextCursor is null once fewer than the page size is returned`(): Unit = runBlocking {
        val messages = listOf(
            message("1", "2026-01-01T00:00:00.000Z"),
            message("2", "2026-01-01T00:01:00.000Z"),
        )
        val useCase = GetMergedConsultationHistoryUseCase(FakeThreadMessageRepository(messages), FakeStorageRepository(), FakeReadStateRepository())

        val response = useCase("doc-1", "pat-1", before = null, size = 50, requesterId = "doc-1")

        assertEquals(2, response.data?.items?.size)
        assertNull(response.data?.nextCursor)
    }

    @Test
    fun `leaves already-hosted https file urls untouched`(): Unit = runBlocking {
        // Note: the non-https (R2 key) resolution branch calls AppConfig.r2, which requires real
        // Cloudflare R2 env vars — untestable here without those set, same pre-existing constraint
        // as the (also untested) GetConsultationHistoryUseCase this mirrors. Only the pass-through
        // branch is exercised in this unit test suite.
        val httpsFile = ConsultationFile("already-hosted.pdf", "https://existing.example/file.pdf", "application/pdf", 100L)
        val messages = listOf(message("1", "2026-01-01T00:00:00.000Z", files = listOf(httpsFile)))
        val useCase = GetMergedConsultationHistoryUseCase(FakeThreadMessageRepository(messages), FakeStorageRepository(), FakeReadStateRepository())

        val response = useCase("doc-1", "pat-1", before = null, size = 10, requesterId = "doc-1")

        assertEquals("https://existing.example/file.pdf", response.data?.items?.first()?.files?.first()?.url)
    }

    // ── Counterpart watermark selection (ticks are computed relative to the OTHER party) ────────

    @Test
    fun `doctor requester sees the patient's watermarks as the counterpart's`(): Unit = runBlocking {
        val readState = ConsultationThreadReadStateEntity(
            doctorId = "doc-1", patientId = "pat-1",
            doctorLastReadAt = "2026-01-01T00:00:00.000Z", doctorLastDeliveredAt = "2026-01-01T00:00:00.000Z",
            patientLastReadAt = "2026-01-02T00:00:00.000Z", patientLastDeliveredAt = "2026-01-01T12:00:00.000Z",
        )
        val useCase = GetMergedConsultationHistoryUseCase(FakeThreadMessageRepository(emptyList()), FakeStorageRepository(), FakeReadStateRepository(readState))

        val response = useCase("doc-1", "pat-1", before = null, size = 10, requesterId = "doc-1")

        assertEquals("2026-01-02T00:00:00.000Z", response.data?.counterpartLastReadAt)
        assertEquals("2026-01-01T12:00:00.000Z", response.data?.counterpartLastDeliveredAt)
    }

    @Test
    fun `patient requester sees the doctor's watermarks as the counterpart's`(): Unit = runBlocking {
        val readState = ConsultationThreadReadStateEntity(
            doctorId = "doc-1", patientId = "pat-1",
            doctorLastReadAt = "2026-01-01T00:00:00.000Z", doctorLastDeliveredAt = "2026-01-01T00:00:00.000Z",
            patientLastReadAt = "2026-01-02T00:00:00.000Z", patientLastDeliveredAt = "2026-01-01T12:00:00.000Z",
        )
        val useCase = GetMergedConsultationHistoryUseCase(FakeThreadMessageRepository(emptyList()), FakeStorageRepository(), FakeReadStateRepository(readState))

        val response = useCase("doc-1", "pat-1", before = null, size = 10, requesterId = "pat-1")

        assertEquals("2026-01-01T00:00:00.000Z", response.data?.counterpartLastReadAt)
        assertEquals("2026-01-01T00:00:00.000Z", response.data?.counterpartLastDeliveredAt)
    }
}
