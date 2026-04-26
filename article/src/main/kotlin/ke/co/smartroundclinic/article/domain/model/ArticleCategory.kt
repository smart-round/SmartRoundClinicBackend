package ke.co.smartroundclinic.article.domain.model

data class ArticleCategory(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String?,
)
