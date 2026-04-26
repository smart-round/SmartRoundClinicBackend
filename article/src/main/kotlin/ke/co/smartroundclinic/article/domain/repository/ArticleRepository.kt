package ke.co.smartroundclinic.article.domain.repository

import ke.co.smartroundclinic.article.data.entity.ArticleEntity
import ke.co.smartroundclinic.common.Resource

interface ArticleRepository {
    suspend fun create(entity: ArticleEntity): Resource<ArticleEntity?>
    suspend fun getById(id: String): Resource<ArticleEntity?>
    suspend fun getAll(page: Int, size: Int, liveOnly: Boolean): Resource<Pair<List<ArticleEntity>, Long>>
    suspend fun getByDoctor(doctorId: String, page: Int, size: Int): Resource<Pair<List<ArticleEntity>, Long>>
    suspend fun getByCategory(categoryId: String, page: Int, size: Int): Resource<Pair<List<ArticleEntity>, Long>>
    suspend fun update(
        id: String,
        title: String?,
        content: String?,
        summary: String?,
        categoryId: String?,
        thumbnailKey: String?,
    ): Resource<ArticleEntity?>
    suspend fun updateByDoctor(
        id: String,
        doctorId: String,
        title: String?,
        content: String?,
        summary: String?,
        categoryId: String?,
        thumbnailKey: String?,
    ): Resource<ArticleEntity?>
    suspend fun publish(id: String): Resource<ArticleEntity?>
    suspend fun publishByDoctor(id: String, doctorId: String): Resource<ArticleEntity?>
    suspend fun suspendArticle(id: String): Resource<ArticleEntity?>
    suspend fun suspendByDoctor(id: String, doctorId: String): Resource<ArticleEntity?>
    suspend fun delete(id: String): Resource<ArticleEntity?>
    suspend fun deleteByDoctor(id: String, doctorId: String): Resource<ArticleEntity?>
}
