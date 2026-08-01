package ke.co.smartroundclinic.doctorchat.domain.usecase

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.VerifiedDoctorResolver
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatThreadEntity
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatThreadRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InitiateDoctorChatUseCaseTest {

    private class FakeDoctorChatThreadRepository : DoctorChatThreadRepository {
        val created = mutableListOf<DoctorChatThreadEntity>()
        override suspend fun getOrCreate(doctorAId: String, doctorBId: String): Resource<DoctorChatThreadEntity> {
            val (a, b) = if (doctorAId <= doctorBId) doctorAId to doctorBId else doctorBId to doctorAId
            val existing = created.find { it.doctorAId == a && it.doctorBId == b }
            if (existing != null) return Resource.Success(existing)
            val entity = DoctorChatThreadEntity(id = "thread-${created.size + 1}", doctorAId = a, doctorBId = b)
            created.add(entity)
            return Resource.Success(entity)
        }
        override suspend fun getById(threadId: String) = Resource.Success<DoctorChatThreadEntity?>(created.find { it.id == threadId })
        override suspend fun getByVideoRoomId(videoRoomId: String) = Resource.Success<DoctorChatThreadEntity?>(null)
        override suspend fun getThreadsForDoctor(doctorId: String) = Resource.Success(emptyList<DoctorChatThreadEntity>())
        override suspend fun setVideoRoomId(threadId: String, videoRoomId: String) = Resource.Success<DoctorChatThreadEntity?>(null)
        override suspend fun setVideoRoomIdIfAbsent(threadId: String, videoRoomId: String) = Resource.Success(videoRoomId)
        override suspend fun clearVideoRoomId(threadId: String, completedRoomId: String?) = Resource.Success(Unit)
    }

    private class FakeVerifiedDoctorResolver(private val verified: Set<String>) : VerifiedDoctorResolver {
        override suspend fun isVerified(doctorId: String) = doctorId in verified
    }

    @Test
    fun `rejects starting a chat with yourself`(): Unit = runBlocking {
        val useCase = InitiateDoctorChatUseCase(FakeDoctorChatThreadRepository(), FakeVerifiedDoctorResolver(setOf("doc-1")))

        val result = useCase("doc-1", "doc-1")

        assertEquals(400, result.httpStatusCode)
    }

    @Test
    fun `rejects when either doctor is not verified`(): Unit = runBlocking {
        val useCase = InitiateDoctorChatUseCase(FakeDoctorChatThreadRepository(), FakeVerifiedDoctorResolver(setOf("doc-1")))

        val result = useCase("doc-1", "doc-2")

        assertEquals(403, result.httpStatusCode)
    }

    @Test
    fun `finds or creates the same thread regardless of caller order`(): Unit = runBlocking {
        val repository = FakeDoctorChatThreadRepository()
        val useCase = InitiateDoctorChatUseCase(repository, FakeVerifiedDoctorResolver(setOf("doc-1", "doc-2")))

        val first = useCase("doc-1", "doc-2")
        val second = useCase("doc-2", "doc-1")

        assertTrue(first.status)
        assertTrue(second.status)
        assertEquals(1, repository.created.size)
    }

    @Test
    fun `succeeds when no verified-doctor resolver is wired`(): Unit = runBlocking {
        val useCase = InitiateDoctorChatUseCase(FakeDoctorChatThreadRepository(), verifiedDoctorResolver = null)

        val result = useCase("doc-1", "doc-2")

        assertTrue(result.status)
        assertFalse(result.data == null)
    }
}
