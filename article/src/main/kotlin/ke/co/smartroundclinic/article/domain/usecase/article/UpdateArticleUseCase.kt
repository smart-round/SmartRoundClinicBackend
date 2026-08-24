package ke.co.smartroundclinic.article.domain.usecase.article

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.article.data.entity.toEntity
import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.article.presentation.dto.request.UpdateArticleReq
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.article.presentation.dto.toModel
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateArticleUseCase(
    private val repository: ArticleRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        id: String,
        doctorId: String,
        req: UpdateArticleReq,
        imageBytes: ByteArray?,
        imageContentType: String?,
    ): DefaultResponse<ArticleRes?> = withContext(Dispatchers.IO) {
        req.references?.let { refs -> validateReferences(refs)?.let { return@withContext it } }

        // Store the R2 key only — presigned URL is generated on each read by ArticleService
        val thumbnailKey = when {
            imageBytes != null && imageContentType != null -> {
                val extension = imageExtensionOrNull(imageContentType) ?: "jpeg"
                val key = "article-thumbnails/$id.$extension"
                val upload = storageRepository.upload(AppConfig.r2.bucket, key, imageBytes, imageContentType)
                if (upload is Resource.Error) return@withContext upload.toDefaultResponse(
                    failedStatusCode = HttpStatusCode.InternalServerError.value,
                    errorMessage = "Failed to upload thumbnail"
                ) { null }
                key
            }
            req.thumbnailUrl != null -> req.thumbnailUrl
            else -> null
        }

        repository.updateByDoctor(
            id = id,
            doctorId = doctorId,
            title = req.title,
            content = req.content,
            summary = req.summary,
            categoryId = req.categoryId,
            thumbnailKey = thumbnailKey,
            references = req.references?.map { it.toModel().toEntity() },
        ).toDefaultResponse { it?.toModel()?.toRes() }
    }
}
