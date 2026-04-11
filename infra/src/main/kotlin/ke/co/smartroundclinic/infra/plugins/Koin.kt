package ke.co.smartroundclinic.infra.plugins

import io.ktor.server.application.*
import ke.co.smartroundclinic.infra.koin.databaseModule
import ke.co.smartroundclinic.infra.koin.httpModule
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.core.module.Module

fun Application.configureKoin(appModules: List<Module>) {
    install(Koin) {
        slf4jLogger()
        modules(appModules)
        createEagerInstances()
    }
}
