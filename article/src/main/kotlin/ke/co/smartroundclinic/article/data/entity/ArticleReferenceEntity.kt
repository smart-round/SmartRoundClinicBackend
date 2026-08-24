package ke.co.smartroundclinic.article.data.entity

import ke.co.smartroundclinic.article.domain.model.ArticleReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Also the JSON shape persisted in [ArticleEntity.referencesJson] — see that field for why. */
@Serializable
data class ArticleReferenceEntity(
    val id: String,
    val title: String,
    val authors: String? = null,
    val publisher: String? = null,
    val url: String,
    val year: Int? = null,
)

private val referencesFormat = Json { ignoreUnknownKeys = true }

fun List<ArticleReferenceEntity>.toReferencesJson(): String = referencesFormat.encodeToString(this)

fun referencesFromJson(json: String?): List<ArticleReferenceEntity> =
    if (json == null) emptyList()
    else runCatching { referencesFormat.decodeFromString<List<ArticleReferenceEntity>>(json) }.getOrDefault(emptyList())

fun ArticleReferenceEntity.toModel() = ArticleReference(
    id = id,
    title = title,
    authors = authors,
    publisher = publisher,
    url = url,
    year = year,
)

fun ArticleReference.toEntity() = ArticleReferenceEntity(
    id = id,
    title = title,
    authors = authors,
    publisher = publisher,
    url = url,
    year = year,
)
