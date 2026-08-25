package ke.co.smartroundclinic.article

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.article.domain.service.ArticleCategoryService
import ke.co.smartroundclinic.article.domain.service.ArticleService
import ke.co.smartroundclinic.article.presentation.controller.articleCategoryController
import ke.co.smartroundclinic.article.presentation.controller.articleController
import ke.co.smartroundclinic.article.presentation.controller.articleShareController
import org.koin.ktor.ext.inject

fun Application.articleModule() {
    val articleCategoryService: ArticleCategoryService by inject()
    val articleService: ArticleService by inject()
    routing {
        articleCategoryController(articleCategoryService)
        articleController(articleService)
        articleShareController(articleService)
    }
}
