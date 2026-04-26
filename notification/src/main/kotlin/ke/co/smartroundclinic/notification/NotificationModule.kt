package ke.co.smartroundclinic.notification

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.notification.domain.service.NotificationService
import ke.co.smartroundclinic.notification.presentation.controller.notificationController
import org.koin.ktor.ext.inject

fun Application.notificationModule() {
    val service by inject<NotificationService>()
    routing {
        notificationController(service)
    }
}
