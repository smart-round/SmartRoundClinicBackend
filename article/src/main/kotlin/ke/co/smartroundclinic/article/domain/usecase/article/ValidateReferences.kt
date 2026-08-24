package ke.co.smartroundclinic.article.domain.usecase.article

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.article.presentation.dto.ArticleReferenceDto
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource

/**
 * Defence-in-depth check behind the app's own form validation — a citation is only useful to a
 * reader if it has a title and a real link, so reject anything else before it reaches the database.
 */
internal fun validateReferences(references: List<ArticleReferenceDto>): DefaultResponse<ArticleRes?>? {
    references.forEach { ref ->
        if (ref.title.isBlank())
            return Resource.Error<ArticleRes?>("Each reference needs a title")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.BadRequest.value) { null }
        val url = ref.url.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            return Resource.Error<ArticleRes?>("Reference \"${ref.title}\" needs a valid link starting with http:// or https://")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.BadRequest.value) { null }
    }
    return null
}
