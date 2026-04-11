package ke.co.smartroundclinic.infra

import io.ktor.server.application.Application
import ke.co.smartroundclinic.infra.plugins.configureHTTP
import ke.co.smartroundclinic.infra.plugins.configureKoin
import ke.co.smartroundclinic.infra.plugins.configureMonitoring
import ke.co.smartroundclinic.infra.plugins.configureSecurity
import ke.co.smartroundclinic.infra.plugins.configureSerialization
import ke.co.smartroundclinic.infra.plugins.configureSockets
import org.koin.core.module.Module

fun Application.configureInfraModule(appModules: List<Module> = emptyList()) {
    configureHTTP()
    configureKoin(appModules)
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureSockets()
}