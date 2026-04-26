package ke.co.smartroundclinic.article.domain.usecase.article

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.article.data.entity.toEntity
import ke.co.smartroundclinic.article.domain.repository.ArticleCategoryRepository
import ke.co.smartroundclinic.article.domain.repository.ArticleRepository
import ke.co.smartroundclinic.article.presentation.dto.request.CreateArticleReq
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes
import ke.co.smartroundclinic.article.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateArticleUseCase(
    private val repository: ArticleRepository,
    private val categoryRepository: ArticleCategoryRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        req: CreateArticleReq,
        doctorId: String,
        imageBytes: ByteArray?,
        imageContentType: String?,
    ): DefaultResponse<ArticleRes?> = withContext(Dispatchers.IO) {
        val categoryCheck = categoryRepository.getById(req.categoryId)
        if (categoryCheck.data == null)
            return@withContext Resource.Error<ArticleRes?>("Article category not found")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.NotFound.value) { null }

        val model = req.toModel(doctorId)

        // Store the R2 key only — presigned URL is generated on each read by ArticleService
        val thumbnailKey = when {
            imageBytes != null && imageContentType != null -> {
                val extension = imageExtensionOrNull(imageContentType) ?: "jpeg"
                val key = "article-thumbnails/${model.id}.$extension"
                val upload = storageRepository.upload(AppConfig.r2.bucket, key, imageBytes, imageContentType)
                if (upload is Resource.Error) return@withContext upload.toDefaultResponse(
                    failedStatusCode = HttpStatusCode.InternalServerError.value,
                    errorMessage = "Failed to upload thumbnail"
                ) { null }
                key
            }
            // caller supplied a direct external URL — store as-is, service detects and skips presigning
            req.thumbnailUrl != null -> req.thumbnailUrl
            else -> null
        }

        repository.create(model.copy(thumbnailKey = thumbnailKey).toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { it?.toModel()?.toRes() }
    }
}
