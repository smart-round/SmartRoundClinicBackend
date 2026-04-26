package ke.co.smartroundclinic.article.domain.repository

import ke.co.smartroundclinic.article.data.entity.ArticleCategoryEntity
import ke.co.smartroundclinic.common.Resource

interface ArticleCategoryRepository {
    suspend fun create(entity: ArticleCategoryEntity): Resource<ArticleCategoryEntity?>
    suspend fun getById(id: String): Resource<ArticleCategoryEntity?>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<ArticleCategoryEntity>, Long>>
    suspend fun update(id: String, name: String?): Resource<ArticleCategoryEntity?>
    suspend fun toggleActive(id: String): Resource<ArticleCategoryEntity?>
    suspend fun delete(id: String): Resource<ArticleCategoryEntity?>
}
