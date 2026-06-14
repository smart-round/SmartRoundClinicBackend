package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.data.entity.ConsultationMessageEntity
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationMessagePageRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository

private const val FILE_URL_TTL = 86400L  // 24 hours

class GetConsultationHistoryUseCase(
    private val repository: ConsultationMessageRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        consultationId: String,
        page: Int,
        size: Int,
    ): DefaultResponse<ConsultationMessagePageRes?> {
        val result = repository.getByConsultationId(consultationId, page, size)
        val mapped = (result as? Resource.Success)?.data?.let { (items, total) ->
            ConsultationMessagePageRes(
                items = items.map { resolveEntityFiles(it).toModel().toRes() },
                total = total,
                page = page,
                size = size,
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
