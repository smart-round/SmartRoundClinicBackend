package ke.co.smartroundclinic.article.domain.service

import ke.co.smartroundclinic.article.data.lookup.DoctorNameLookup
import ke.co.smartroundclinic.article.domain.usecase.article.CreateArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetDeletedArticlesUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.DeleteArticleByDoctorUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.DeleteArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetAllArticlesUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetArticleByIdUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetArticlesByCategoryUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.GetArticlesByDoctorUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.PublishArticleByDoctorUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.PublishArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.SuspendArticleByDoctorUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.SuspendArticleUseCase
import ke.co.smartroundclinic.article.domain.usecase.article.UpdateArticleUseCase
import ke.co.smartroundclinic.article.presentation.dto.request.CreateArticleReq
import ke.co.smartroundclinic.article.presentation.dto.request.UpdateArticleReq
import ke.co.smartroundclinic.article.presentation.dto.response.ArticlePageResult
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository

private const val THUMBNAIL_TTL = 604800L // 7 days

class ArticleService(
    private val createUseCase: CreateArticleUseCase,
    private val getByIdUseCase: GetArticleByIdUseCase,
    private val getAllUseCase: GetAllArticlesUseCase,
    private val getByCategoryUseCase: GetArticlesByCategoryUseCase,
    private val getByDoctorUseCase: GetArticlesByDoctorUseCase,
    private val updateUseCase: UpdateArticleUseCase,
    private val publishUseCase: PublishArticleUseCase,
    private val publishByDoctorUseCase: PublishArticleByDoctorUseCase,
    private val suspendUseCase: SuspendArticleUseCase,
    private val suspendByDoctorUseCase: SuspendArticleByDoctorUseCase,
    private val deleteUseCase: DeleteArticleUseCase,
    private val deleteByDoctorUseCase: DeleteArticleByDoctorUseCase,
    private val getDeletedUseCase: GetDeletedArticlesUseCase,
    private val storageRepository: StorageRepository,
    private val doctorNameLookup: DoctorNameLookup,
) {
    suspend fun create(req: CreateArticleReq, doctorId: String, imageBytes: ByteArray?, imageContentType: String?) =
        createUseCase(req, doctorId, imageBytes, imageContentType).withThumbnail().withAuthorName()

    suspend fun getById(id: String) = getByIdUseCase(id).withThumbnail().withAuthorName()

    suspend fun getAll(page: Int, size: Int, liveOnly: Boolean) = getAllUseCase(page, size, liveOnly).withThumbnails().withAuthorNames()

    suspend fun getByCategory(categoryId: String, page: Int, size: Int) =
        getByCategoryUseCase(categoryId, page, size).withThumbnails().withAuthorNames()

    suspend fun getByDoctor(doctorId: String, page: Int, size: Int) =
        getByDoctorUseCase(doctorId, page, size).withThumbnails().withAuthorNames()

    suspend fun update(id: String, doctorId: String, req: UpdateArticleReq, imageBytes: ByteArray?, imageContentType: String?) =
        updateUseCase(id, doctorId, req, imageBytes, imageContentType).withThumbnail().withAuthorName()

    suspend fun publish(id: String) = publishUseCase(id).withThumbnail().withAuthorName()
    suspend fun publishByDoctor(id: String, doctorId: String) = publishByDoctorUseCase(id, doctorId).withThumbnail().withAuthorName()

    suspend fun suspendArticle(id: String) = suspendUseCase(id).withThumbnail().withAuthorName()
    suspend fun suspendByDoctor(id: String, doctorId: String) = suspendByDoctorUseCase(id, doctorId).withThumbnail().withAuthorName()

    suspend fun delete(id: String) = deleteUseCase(id).withThumbnail().withAuthorName()
    suspend fun deleteByDoctor(id: String, doctorId: String) = deleteByDoctorUseCase(id, doctorId).withThumbnail().withAuthorName()
    suspend fun getDeleted(page: Int, size: Int) = getDeletedUseCase(page, size).withThumbnails().withAuthorNames()

    // Generates a fresh presigned URL from an R2 key. External URLs (http/https) are passed through unchanged.
    private suspend fun resolveUrl(keyOrUrl: String?): String? {
        if (keyOrUrl == null) return null
        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) return keyOrUrl
        return storageRepository.presignedGetUrl(AppConfig.r2.bucket, keyOrUrl, THUMBNAIL_TTL).data
    }

    private suspend fun DefaultResponse<ArticleRes?>.withThumbnail(): DefaultResponse<ArticleRes?> {
        val d = data ?: return this
        return copy(data = d.copy(thumbnailUrl = resolveUrl(d.thumbnailUrl)))
    }

    private suspend fun DefaultResponse<ArticlePageResult?>.withThumbnails(): DefaultResponse<ArticlePageResult?> {
        val d = data ?: return this
        return copy(data = d.copy(items = d.items.map { it.copy(thumbnailUrl = resolveUrl(it.thumbnailUrl)) }))
    }

    private suspend fun DefaultResponse<ArticleRes?>.withAuthorName(): DefaultResponse<ArticleRes?> {
        val d = data ?: return this
        return copy(data = d.copy(authorName = doctorNameLookup.lookup(d.doctorId)))
    }

    private suspend fun DefaultResponse<ArticlePageResult?>.withAuthorNames(): DefaultResponse<ArticlePageResult?> {
        val d = data ?: return this
        val names = doctorNameLookup.bulkLookup(d.items.map { it.doctorId }.toSet())
        return copy(data = d.copy(items = d.items.map { it.copy(authorName = names[it.doctorId]) }))
    }
}
