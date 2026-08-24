package ke.co.smartroundclinic.article.data.entity

import ke.co.smartroundclinic.article.domain.model.Article
import ke.co.smartroundclinic.article.domain.model.ArticleState
import org.bson.types.ObjectId
import kotlin.time.Clock

data class ArticleEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val title: String,
    val content: String,
    val summary: String,
    val categoryId: String,
    val thumbnailKey: String? = null,
    val state: String = ArticleState.DRAFT.name,
    val datePosted: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
    // Stored as a JSON string rather than a native BSON array of ArticleReferenceEntity: the
    // MongoDB Kotlin driver's default-value support for a missing field is solid for simple
    // nullable/scalar properties (every other optional field here is one) but is a materially
    // bigger ask for a missing field typed as List<DataClass> — a String field with a "[]"
    // default is the same well-trodden shape as every other optional column, so legacy documents
    // written before this field existed decode exactly like they always have.
    val referencesJson: String = "[]",
) {
    fun toModel() = Article(
        id = id,
        doctorId = doctorId,
        title = title,
        content = content,
        summary = summary,
        categoryId = categoryId,
        thumbnailKey = thumbnailKey,
        state = runCatching { ArticleState.valueOf(state) }.getOrDefault(ArticleState.DRAFT),
        datePosted = datePosted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        references = referencesFromJson(referencesJson).map { it.toModel() },
    )
}

fun Article.toEntity() = ArticleEntity(
    id = id,
    doctorId = doctorId,
    title = title,
    content = content,
    summary = summary,
    categoryId = categoryId,
    thumbnailKey = thumbnailKey,
    state = state.name,
    datePosted = datePosted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    referencesJson = references.map { it.toEntity() }.toReferencesJson(),
)
