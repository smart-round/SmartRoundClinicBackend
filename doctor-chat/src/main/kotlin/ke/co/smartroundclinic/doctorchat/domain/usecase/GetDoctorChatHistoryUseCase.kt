package ke.co.smartroundclinic.doctorchat.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageCursor
import ke.co.smartroundclinic.doctorchat.domain.repository.DoctorChatMessageRepository
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatService
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorChatMessagesPageRes
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.toRes
import java.util.Base64

/** Cursor-paginated message history for a single doctor-chat thread. */
class GetDoctorChatHistoryUseCase(
    private val repository: DoctorChatMessageRepository,
    private val chatService: DoctorChatService,
) {
    suspend operator fun invoke(threadId: String, before: String?, size: Int): DefaultResponse<DoctorChatMessagesPageRes?> {
        val cursor = before?.let(::decodeCursor)
        val result = repository.getByThread(threadId, cursor, size)

        val mapped = (result as? Resource.Success)?.data?.let { items ->
            DoctorChatMessagesPageRes(
                items = items.map { chatService.resolveEntityFiles(it).toModel().toRes() },
                nextCursor = if (items.size < size) null else items.last().let { encodeCursor(it.createdAt, it.id) },
            )
        }
        return result.toDefaultResponse { mapped }
    }
}

private fun encodeCursor(createdAt: String, id: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString("$createdAt|$id".toByteArray())

private fun decodeCursor(raw: String): DoctorChatMessageCursor? = runCatching {
    val decoded = String(Base64.getUrlDecoder().decode(raw))
    val (createdAt, id) = decoded.split("|", limit = 2)
    DoctorChatMessageCursor(createdAt = createdAt, id = id)
}.getOrNull()
