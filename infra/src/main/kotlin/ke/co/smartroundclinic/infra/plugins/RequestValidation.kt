package ke.co.smartroundclinic.infra.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationConfig

fun Application.configureRequestValidation(configure: RequestValidationConfig.() -> Unit = {}) {
    install(RequestValidation, configure)
}