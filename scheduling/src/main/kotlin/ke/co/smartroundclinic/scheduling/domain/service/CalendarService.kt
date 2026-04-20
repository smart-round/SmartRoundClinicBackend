package ke.co.smartroundclinic.scheduling.domain.service

import ke.co.smartroundclinic.scheduling.data.entity.AppointmentEntity
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.GetCalendarRangeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class CalendarService(
    private val getCalendarRangeUseCase: GetCalendarRangeUseCase,
    private val appointmentRepository: AppointmentRepository,
) {
    suspend fun getCalendar(doctorId: String, from: LocalDate, to: LocalDate, forDoctor: Boolean) =
        getCalendarRangeUseCase(doctorId, from, to, forDoctor)

    fun watchAppointments(doctorId: String): Flow<AppointmentEntity> =
        appointmentRepository.watchByDoctorId(doctorId)
}
