package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationThreadReadStateEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadReadStateRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkThreadReadUseCaseTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    private class FakeReadStateRepository : ConsultationThreadReadStateRepository {
        data class MarkReadCall(val doctorId: String, val patientId: String, val readerId: String)
        val markReadCalls = mutableListOf<MarkReadCall>()

        override suspend fun getByPair(doctorId: String, patientId: String) =
            Resource.Success<ConsultationThreadReadStateEntity?>(null)
        override suspend fun getByPairs(pairs: List<Pair<String, String>>) = Resource.Success(emptyList<ConsultationThreadReadStateEntity>())

        override suspend fun markRead(doctorId: String, patientId: String, readerId: String, at: String): Resource<ConsultationThreadReadStateEntity?> {
            markReadCalls += MarkReadCall(doctorId, patientId, readerId)
            val entity = ConsultationThreadReadStateEntity(
                doctorId = doctorId, patientId = patientId,
                doctorLastReadAt = if (readerId == doctorId) at else null,
                patientLastReadAt = if (readerId == patientId) at else null,
            )
            return Resource.Success(entity)
        }

        override suspend fun bumpDelivered(doctorId: String, patientId: String, recipientId: String, at: String) =
            Resource.Success<ConsultationThreadReadStateEntity?>(null)
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `doctor reader advances doctorLastReadAt`(): Unit = runBlocking {
        val repo = FakeReadStateRepository()
        val useCase = MarkThreadReadUseCase(repo)

        val result = useCase("doc-1", "pat-1", readerId = "doc-1")

        assertEquals(1, repo.markReadCalls.size)
        assertEquals(FakeReadStateRepository.MarkReadCall("doc-1", "pat-1", "doc-1"), repo.markReadCalls.first())
        assertEquals(true, (result as? Resource.Success)?.data?.doctorLastReadAt != null)
        assertEquals(null, (result as? Resource.Success)?.data?.patientLastReadAt)
    }

    @Test
    fun `patient reader advances patientLastReadAt`(): Unit = runBlocking {
        val repo = FakeReadStateRepository()
        val useCase = MarkThreadReadUseCase(repo)

        val result = useCase("doc-1", "pat-1", readerId = "pat-1")

        assertEquals(1, repo.markReadCalls.size)
        assertEquals(FakeReadStateRepository.MarkReadCall("doc-1", "pat-1", "pat-1"), repo.markReadCalls.first())
        assertEquals(true, (result as? Resource.Success)?.data?.patientLastReadAt != null)
        assertEquals(null, (result as? Resource.Success)?.data?.doctorLastReadAt)
    }
}
