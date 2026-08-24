package ke.co.smartroundclinic.article.domain.model

/** A citation a doctor attaches to back a medical claim — journal, guideline, or publisher source. */
data class ArticleReference(
    val id: String,
    val title: String,
    val authors: String?,
    val publisher: String?,
    val url: String,
    val year: Int?,
)
