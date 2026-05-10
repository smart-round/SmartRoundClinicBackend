package ke.co.smartroundclinic.scheduling.koin

import com.mongodb.kotlin.client.coroutine.MongoClient
import ke.co.smartroundclinic.scheduling.data.repository.AppointmentRepositoryImpl
import ke.co.smartroundclinic.scheduling.data.repository.DoctorScheduleRepositoryImpl
import ke.co.smartroundclinic.scheduling.data.repository.SlotOverrideRepositoryImpl
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.domain.repository.SlotOverrideRepository
import ke.co.smartroundclinic.scheduling.domain.service.AppointmentService
import ke.co.smartroundclinic.scheduling.domain.service.CalendarService
import ke.co.smartroundclinic.scheduling.domain.service.ScheduleService
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.BookAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.CancelAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.CompleteAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.ConfirmAppointmentUseCase
import ke.co.smartroundclinic.common.DoctorSpecialitiesResolver
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAllAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetDoctorAppointmentDetailsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAppointmentByIdUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetDoctorAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetPatientAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.MarkNoShowUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.DeactivateDayUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.GetAvailableSlotsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.GetCalendarRangeUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.GetScheduleUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.UpdateScheduleDayUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.UpsertScheduleUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val schedulingKoinModule = module {
    single<AppointmentRepository> { AppointmentRepositoryImpl(get<MongoClient>(), get(named("schedulingDb"))) }
    single<DoctorScheduleRepository> { DoctorScheduleRepositoryImpl(get(named("schedulingDb"))) }
    single<SlotOverrideRepository> { SlotOverrideRepositoryImpl(get(named("schedulingDb"))) }

    // Appointment use cases
    single { BookAppointmentUseCase(get(), get(), get()) }
    single { GetAllAppointmentsUseCase(get()) }
    single { GetDoctorAppointmentDetailsUseCase(get(), getOrNull<PatientNameResolver>(), getOrNull<DoctorSpecialitiesResolver>()) }
    single { GetAppointmentByIdUseCase(get()) }
    single { GetPatientAppointmentsUseCase(get()) }
    single { GetDoctorAppointmentsUseCase(get()) }
    single { ConfirmAppointmentUseCase(get()) }
    single { CancelAppointmentUseCase(get()) }
    single { CompleteAppointmentUseCase(get()) }
    single { MarkNoShowUseCase(get()) }

    // Schedule use cases
    single { UpsertScheduleUseCase(get()) }
    single { GetScheduleUseCase(get()) }
    single { UpdateScheduleDayUseCase(get()) }
    single { DeactivateDayUseCase(get()) }
    single { GetAvailableSlotsUseCase(get(), get(), get()) }
    single { GetCalendarRangeUseCase(get(), get(), get()) }

    // Services
    single { AppointmentService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { ScheduleService(get(), get(), get(), get(), get()) }
    single { CalendarService(get(), get()) }
}
