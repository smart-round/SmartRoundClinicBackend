package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.NextAppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toNextAppointmentRes
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val APPOINTMENT_TIMEZONE = TimeZone.of("Africa/Nairobi")

/**
 * Chat threads are permanent per doctor-patient pair and can span many appointments over time, so
 * clients can't reliably pick "the relevant one" out of a locally cached list. This is the single
 * source of truth: the soonest CONFIRMED appointment that hasn't already passed, used to decide
 * whether/when a client should offer the video-call option for a thread.
 */
class GetNextAppointmentUseCase(private val repository: AppointmentRepository) {
    suspend operator fun invoke(doctorId: String, patientId: String): DefaultResponse<NextAppointmentRes?> {
        val today = Clock.System.now().toLocalDateTime(APPOINTMENT_TIMEZONE).date.toString()
        return repository.getNextConfirmedAppointment(doctorId, patientId, today)
            .toDefaultResponse { it?.toModel()?.toNextAppointmentRes() }
    }
}
