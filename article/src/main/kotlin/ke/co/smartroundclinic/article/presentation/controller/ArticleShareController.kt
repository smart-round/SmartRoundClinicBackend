package ke.co.smartroundclinic.article.presentation.controller

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import ke.co.smartroundclinic.article.domain.service.ArticleService
import ke.co.smartroundclinic.article.presentation.dto.response.ArticleRes

private const val SHARE_BASE_URL = "https://api.smartroundclinic.co.ke/a"
private const val DEFAULT_DESCRIPTION =
    "Medical articles from licensed doctors on SmartRound Clinic — read, verify the sources, and share."

/**
 * Unauthenticated share/preview page for a single article, at /a/{id}. This is what a link
 * unfurler (WhatsApp, iMessage, Slack) fetches to build a rich preview when a doctor or patient
 * shares an article link, and what a browser lands on if the app isn't installed — an installed
 * app never hits this at all, App Links / Universal Links intercept the tap first.
 */
fun Route.articleShareController(service: ArticleService) {
    get("/a/{id}") {
        val id = call.parameters["id"]
        val article = id?.let { service.getById(it).data }
        call.respondText(renderSharePage(id, article), ContentType.Text.Html)
    }
}

private fun renderSharePage(id: String?, article: ArticleRes?): String {
    val canonicalUrl = "$SHARE_BASE_URL/${id.orEmpty()}"
    val title = article?.title?.takeIf { it.isNotBlank() } ?: "SmartRound Clinic Article"
    val description = article?.summary?.takeIf { it.isNotBlank() }?.let(::truncate) ?: DEFAULT_DESCRIPTION
    val image = article?.thumbnailUrl

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${esc(title)}</title>
            <meta property="og:type" content="article">
            <meta property="og:title" content="${esc(title)}">
            <meta property="og:description" content="${esc(description)}">
            <meta property="og:url" content="${esc(canonicalUrl)}">
            <meta property="og:site_name" content="SmartRound Clinic">
            ${if (image != null) """<meta property="og:image" content="${esc(image)}">""" else ""}
            <meta name="twitter:card" content="${if (image != null) "summary_large_image" else "summary"}">
            <meta name="twitter:title" content="${esc(title)}">
            <meta name="twitter:description" content="${esc(description)}">
            ${if (image != null) """<meta name="twitter:image" content="${esc(image)}">""" else ""}
            <style>
                body {
                    margin: 0;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background: #FFF7F5;
                    color: #1F1B1A;
                    display: flex;
                    justify-content: center;
                    padding: 32px 16px;
                }
                .card {
                    max-width: 480px;
                    width: 100%;
                    background: #FFFFFF;
                    border-radius: 16px;
                    overflow: hidden;
                    box-shadow: 0 8px 24px rgba(0,0,0,0.08);
                }
                .banner {
                    background: linear-gradient(135deg, #FF6A3D, #B3261E);
                    color: #fff;
                    padding: 20px 24px;
                    font-weight: 700;
                    letter-spacing: 0.04em;
                    font-size: 13px;
                }
                img.thumb { width: 100%; display: block; aspect-ratio: 16 / 9; object-fit: cover; }
                .body { padding: 24px; }
                h1 { font-size: 22px; line-height: 1.3; margin: 0 0 12px; }
                p { font-size: 15px; line-height: 1.5; color: #4A4441; margin: 0 0 20px; }
                .hint { font-size: 13px; color: #8A817D; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="banner">SMART ROUND CLINIC</div>
                ${if (image != null) """<img class="thumb" src="${esc(image)}" alt="">""" else ""}
                <div class="body">
                    <h1>${esc(title)}</h1>
                    <p>${esc(description)}</p>
                    <div class="hint">Open this link on your phone with the SmartRound Clinic app installed to read the full article.</div>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}

private fun truncate(text: String, max: Int = 200): String =
    if (text.length <= max) text else text.take(max).substringBeforeLast(' ') + "…"

private fun esc(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
