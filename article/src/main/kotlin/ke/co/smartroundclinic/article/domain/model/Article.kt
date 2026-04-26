package ke.co.smartroundclinic.article.domain.model

data class Article(
    val id: String,
    val doctorId: String,
    val title: String,
    val content: String,
    val summary: String,
    val categoryId: String,
    val thumbnailKey: String?,
    val state: ArticleState,
    val datePosted: String?,
    val createdAt: String,
    val updatedAt: String?,
)
