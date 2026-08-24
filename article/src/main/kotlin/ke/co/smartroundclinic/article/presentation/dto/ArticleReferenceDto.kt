package ke.co.smartroundclinic.article.presentation.dto

import ke.co.smartroundclinic.article.domain.model.ArticleReference
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

/**
 * Wire shape for a citation, shared by request parsing (doctor submits a JSON array in the
 * "references" multipart field) and response serialization — the shape is identical both ways.
 */
@Serializable
data class ArticleReferenceDto(
    val id: String? = null,
    val title: String,
    val authors: String? = null,
    val publisher: String? = null,
    val url: String,
    val year: Int? = null,
)

fun ArticleReferenceDto.toModel() = ArticleReference(
    id = id?.takeIf { it.isNotBlank() } ?: ObjectId().toString(),
    title = title.trim(),
    authors = authors?.trim()?.takeIf { it.isNotBlank() },
    publisher = publisher?.trim()?.takeIf { it.isNotBlank() },
    url = url.trim(),
    year = year,
)

fun ArticleReference.toDto() = ArticleReferenceDto(
    id = id,
    title = title,
    authors = authors,
    publisher = publisher,
    url = url,
    year = year,
)
