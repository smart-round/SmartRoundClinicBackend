package ke.co.smartroundclinic.article.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.toByteArray
import ke.co.smartroundclinic.article.domain.service.ArticleService
import ke.co.smartroundclinic.article.presentation.dto.request.CreateArticleReq
import ke.co.smartroundclinic.article.presentation.dto.request.UpdateArticleReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireImageContentType
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"

fun Route.articleController(service: ArticleService) {
    authenticate("auth-jwt") {
        route("/article") {

            // ── Doctor: create article ───────────────────────────────────────
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val parts = mutableMapOf<String, String>()
                    var imageBytes: ByteArray? = null
                    var imageContentType = ""
                    call.receiveMultipart().forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> parts[part.name ?: ""] = part.value
                            is PartData.FileItem -> {
                                imageBytes = part.provider().toByteArray()
                                imageContentType = part.contentType?.toString() ?: ""
                            }

                            else -> Unit
                        }
                        part.dispose()
                    }
                    val title = parts["title"] ?: return@requireRole call.respond(
                        HttpStatusCode.BadRequest, mapOf("message" to "title is required")
                    )
                    val content = parts["content"] ?: return@requireRole call.respond(
                        HttpStatusCode.BadRequest, mapOf("message" to "content is required")
                    )
                    val summary = parts["summary"] ?: return@requireRole call.respond(
                        HttpStatusCode.BadRequest, mapOf("message" to "summary is required")
                    )
                    val categoryId = parts["categoryId"] ?: return@requireRole call.respond(
                        HttpStatusCode.BadRequest, mapOf("message" to "categoryId is required")
                    )
                    if (imageBytes != null) requireImageContentType(imageContentType)
                    val req = CreateArticleReq(
                        title = title,
                        content = content,
                        summary = summary,
                        categoryId = categoryId,
                        thumbnailUrl = parts["thumbnailUrl"],
                    )
                    val result = service.create(req, doctorId, imageBytes, imageContentType.ifBlank { null })
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // ── Doctor: get own articles ─────────────────────────────────────
            route("my") {
                get {
                    call.requireRole(DOCTOR) {
                        val doctorId = call.getUserId() ?: return@requireRole
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                        val result = service.getByDoctor(doctorId, page, size)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // ── Doctor: publish own article ──────────────────────────────────
                patch("publish") {
                    call.requireRole(DOCTOR) {
                        val doctorId = call.getUserId() ?: return@requireRole
                        val id = call.request.queryParameters["id"]
                            ?: throw MissingParametersException("id query parameter is missing")
                        val result = service.publishByDoctor(id, doctorId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
                // ── Doctor: unpublish own article (reverts to DRAFT) ─────────────
                patch("unpublish") {
                    call.requireRole(DOCTOR) {
                        val doctorId = call.getUserId() ?: return@requireRole
                        val id = call.request.queryParameters["id"]
                            ?: throw MissingParametersException("id query parameter is missing")
                        val result = service.suspendByDoctor(id, doctorId)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }


            }

            // ── Doctor: update own article ───────────────────────────────────
            put {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val parts = mutableMapOf<String, String>()
                    var imageBytes: ByteArray? = null
                    var imageContentType = ""
                    call.receiveMultipart().forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> parts[part.name ?: ""] = part.value
                            is PartData.FileItem -> {
                                imageBytes = part.streamProvider().readBytes()
                                imageContentType = part.contentType?.toString() ?: ""
                            }

                            else -> Unit
                        }
                        part.dispose()
                    }
                    if (imageBytes != null) requireImageContentType(imageContentType)
                    val req = UpdateArticleReq(
                        title = parts["title"],
                        content = parts["content"],
                        summary = parts["summary"],
                        categoryId = parts["categoryId"],
                        thumbnailUrl = parts["thumbnailUrl"],
                    )
                    val result = service.update(id, doctorId, req, imageBytes, imageContentType.ifBlank { null })
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // ── Doctor: delete own article ───────────────────────────────────
            delete {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val result = service.deleteByDoctor(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // ── Any JWT: get single article ──────────────────────────────────
            get {
                val id = call.request.queryParameters["id"]
                    ?: throw MissingParametersException("id query parameter is missing")
                val result = service.getById(id)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // ── Any JWT: get all LIVE articles ───────────────────────────────
            route("live") {
                get {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getAll(page, size, liveOnly = true)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }

                // ── Any JWT: get LIVE articles by category ───────────────────────
                get("category") {
                    val categoryId = call.request.queryParameters["categoryId"]
                        ?: throw MissingParametersException("categoryId query parameter is missing")
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getByCategory(categoryId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // ── Admin: get all articles (any state except DELETED) ───────────
            route("admin") {
                get("all") {
                    call.requireRole(ADMIN) {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                        val result = service.getAll(page, size, liveOnly = false)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }


                // ── Admin: publish article ───────────────────────────────────────
                patch("publish") {
                    call.requireRole(ADMIN) {
                        val id = call.request.queryParameters["id"]
                            ?: throw MissingParametersException("id query parameter is missing")
                        val result = service.publish(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // ── Admin: suspend article ───────────────────────────────────────
                patch("suspend") {
                    call.requireRole(ADMIN) {
                        val id = call.request.queryParameters["id"]
                            ?: throw MissingParametersException("id query parameter is missing")
                        val result = service.suspendArticle(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }

                // ── Admin: delete any article (moderation) ───────────────────────
                delete {
                    call.requireRole(ADMIN) {
                        val id = call.request.queryParameters["id"]
                            ?: throw MissingParametersException("id query parameter is missing")
                        val result = service.delete(id)
                        call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                    }
                }
            }
        }
    }
}
