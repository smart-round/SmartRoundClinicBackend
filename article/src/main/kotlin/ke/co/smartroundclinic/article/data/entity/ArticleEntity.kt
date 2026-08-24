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
    // MUST be nullable, not a non-null default: confirmed via direct decode testing against
    // production that the MongoDB Kotlin driver's DataClassCodec passes an explicit null for a
    // field missing from a legacy BSON document rather than invoking the Kotlin default — which
    // throws a constructor NPE for a non-null parameter. Every other optional field on this
    // entity is nullable for the same reason; this one has to be too, with "[]" applied manually
    // in toModel() instead of at the constructor default.
    val referencesJson: String? = null,
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
