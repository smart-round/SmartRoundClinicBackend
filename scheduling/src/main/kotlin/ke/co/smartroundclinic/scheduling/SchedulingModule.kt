package ke.co.smartroundclinic.scheduling

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.scheduling.domain.service.AppointmentService
import ke.co.smartroundclinic.scheduling.domain.service.CalendarService
import ke.co.smartroundclinic.scheduling.domain.service.ScheduleService
import ke.co.smartroundclinic.scheduling.presentation.controller.appointmentController
import ke.co.smartroundclinic.scheduling.presentation.controller.calendarController
import ke.co.smartroundclinic.scheduling.presentation.controller.scheduleController
import org.koin.ktor.ext.inject

fun Application.schedulingModule() {
    val appointmentService: AppointmentService by inject()
    val scheduleService: ScheduleService by inject()
    val calendarService: CalendarService by inject()
    routing {
        appointmentController(appointmentService)
        scheduleController(scheduleService)
        calendarController(calendarService)
    }
}
