package ke.co.smartroundclinic.scheduling.koin

import com.mongodb.kotlin.client.coroutine.MongoClient
import ke.co.smartroundclinic.scheduling.data.lookup.AppointmentAdminLookup
import ke.co.smartroundclinic.scheduling.data.lookup.PaymentVerificationLookup
import ke.co.smartroundclinic.scheduling.data.lookup.RefundLookup
import ke.co.smartroundclinic.scheduling.data.repository.AppointmentRepositoryImpl
import ke.co.smartroundclinic.scheduling.data.repository.DoctorScheduleRepositoryImpl
import ke.co.smartroundclinic.scheduling.data.repository.ServiceTierLookup
import ke.co.smartroundclinic.scheduling.data.repository.SlotOverrideRepositoryImpl
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.domain.repository.SlotOverrideRepository
import ke.co.smartroundclinic.scheduling.domain.service.AdminAppointmentService
import ke.co.smartroundclinic.scheduling.domain.service.AdminRefundService
import ke.co.smartroundclinic.scheduling.domain.service.AppointmentService
import ke.co.smartroundclinic.scheduling.domain.service.CalendarService
import ke.co.smartroundclinic.scheduling.domain.service.ScheduleService
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.BookAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.CancelAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.CompleteAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.ConfirmAppointmentUseCase
import ke.co.smartroundclinic.common.DoctorSpecialitiesResolver
import ke.co.smartroundclinic.common.PatientNameResolver
import ke.co.smartroundclinic.common.UserProfilePictureResolver
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAdminAllAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAdminAppointmentStatsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAllAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetDoctorAppointmentDetailsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAppointmentByIdUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetDoctorAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetNextAppointmentUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetPatientAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetPatientAppointmentsForAdminUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.MarkNoShowUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.refund.GetAdminAllRefundsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.refund.GetRefundByIdUseCase
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
    single { ServiceTierLookup(get(named("adminDb")), get(named("doctorDb"))) }
    single { AppointmentAdminLookup(get(named("authDb")), get(named("paymentsDb"))) }
    single { PaymentVerificationLookup(get(named("paymentsDb"))) }
    single { RefundLookup(get(named("paymentsDb"))) }

    // Appointment use cases
    single { BookAppointmentUseCase(get(), get(), get(), get(), get(), getOrNull()) }
    single { GetAllAppointmentsUseCase(get()) }
    single { GetDoctorAppointmentDetailsUseCase(get(), getOrNull<PatientNameResolver>(), getOrNull<DoctorSpecialitiesResolver>(), getOrNull<UserProfilePictureResolver>()) }
    single { GetAppointmentByIdUseCase(get(), get()) }
    single { GetPatientAppointmentsUseCase(get(), getOrNull<PatientNameResolver>(), getOrNull<UserProfilePictureResolver>(), getOrNull<DoctorSpecialitiesResolver>(), get()) }
    single { GetPatientAppointmentsForAdminUseCase(get(), getOrNull<PatientNameResolver>(), getOrNull<DoctorSpecialitiesResolver>()) }
    single { GetDoctorAppointmentsUseCase(get()) }
    single { GetNextAppointmentUseCase(get()) }
    single { ConfirmAppointmentUseCase(get(), getOrNull()) }
    single { CancelAppointmentUseCase(get(), get(), get(), getOrNull(), getOrNull()) }
    single { CompleteAppointmentUseCase(get(), getOrNull(), getOrNull()) }
    single { MarkNoShowUseCase(get(), getOrNull()) }

    // Schedule use cases
    single { UpsertScheduleUseCase(get()) }
    single { GetScheduleUseCase(get()) }
    single { UpdateScheduleDayUseCase(get()) }
    single { DeactivateDayUseCase(get()) }
    single { GetAvailableSlotsUseCase(get(), get(), get(), get()) }
    single { GetCalendarRangeUseCase(get(), get(), get()) }

    // Admin use cases + service
    single { GetAdminAppointmentStatsUseCase(get(), get()) }
    single { GetAdminAllAppointmentsUseCase(get(), get()) }
    single { AdminAppointmentService(get(), get()) }
    single { GetAdminAllRefundsUseCase(get(), get()) }
    single { GetRefundByIdUseCase(get(), get()) }
    single { AdminRefundService(get(), get()) }

    // Services
    single { AppointmentService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { ScheduleService(get(), get(), get(), get(), get()) }
    single { CalendarService(get(), get()) }
}
