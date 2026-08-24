package ke.co.smartroundclinic.article.data.entity

import ke.co.smartroundclinic.article.domain.model.ArticleReference

data class ArticleReferenceEntity(
    val id: String,
    val title: String,
    val authors: String? = null,
    val publisher: String? = null,
    val url: String,
    val year: Int? = null,
)

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
