package ke.co.smartroundclinic.article.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.article.domain.service.ArticleCategoryService
import ke.co.smartroundclinic.article.presentation.dto.request.CreateArticleCategoryReq
import ke.co.smartroundclinic.article.presentation.dto.request.UpdateArticleCategoryReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"

fun Route.articleCategoryController(service: ArticleCategoryService) {
    authenticate("auth-jwt") {
        route("/article/categories") {
            post {
                call.requireRole(ADMIN) {
                    val body = call.receive<CreateArticleCategoryReq>()
                    val result = service.create(body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("all") {
                call.requireRole(ADMIN,DOCTOR) {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getAll(page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            put {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val body = call.receive<UpdateArticleCategoryReq>()
                    val result = service.update(id, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("toggle") {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val result = service.toggle(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            delete {
                call.requireRole(ADMIN) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id query parameter is missing")
                    val result = service.delete(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
