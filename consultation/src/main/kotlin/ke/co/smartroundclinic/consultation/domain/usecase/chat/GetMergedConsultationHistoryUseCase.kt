package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.MessageCursor
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConversationThreadMessagesRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import java.util.Base64

private const val FILE_URL_TTL = 86400L  // 24 hours

/** Merged, cursor-paginated message history across every consultation a (doctorId, patientId) pair has had. */
class GetMergedConsultationHistoryUseCase(
    private val repository: ConsultationMessageRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        doctorId: String,
        patientId: String,
        before: String?,
        size: Int,
    ): DefaultResponse<ConversationThreadMessagesRes?> {
        val cursor = before?.let(::decodeCursor)
        val result = repository.getByThread(doctorId, patientId, cursor, size)

        val mapped = (result as? Resource.Success)?.data?.let { items ->
            ConversationThreadMessagesRes(
                items = items.map { resolveEntityFiles(it).toModel().toRes() },
                nextCursor = if (items.size < size) null else items.last().let { encodeCursor(it.createdAt, it.id) },
            )
        }
        return result.toDefaultResponse { mapped }
    }

    private suspend fun resolveEntityFiles(entity: ConsultationMessageEntity): ConsultationMessageEntity {
        if (entity.files.isEmpty()) return entity
        val resolved = entity.files.map { file ->
            if (file.url.startsWith("https://")) file
            else {
                val url = (storageRepository.presignedGetUrl(AppConfig.r2.bucket, file.url, FILE_URL_TTL) as? Resource.Success)?.data
                if (url != null) file.copy(url = url) else file
            }
        }
        return entity.copy(files = resolved)
    }
}

private fun encodeCursor(createdAt: String, id: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString("$createdAt|$id".toByteArray())

private fun decodeCursor(raw: String): MessageCursor? = runCatching {
    val decoded = String(Base64.getUrlDecoder().decode(raw))
    val (createdAt, id) = decoded.split("|", limit = 2)
    MessageCursor(createdAt = createdAt, id = id)
}.getOrNull()
