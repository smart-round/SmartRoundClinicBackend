package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.NextAppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toNextAppointmentRes

/**
 * Chat threads are permanent per doctor-patient pair and can span many appointments over time, so
 * clients can't reliably pick "the relevant one" out of a locally cached list. This is the single
 * source of truth: the soonest CONFIRMED appointment that hasn't expired (slotStart + 24h hasn't
 * passed yet), used to decide whether/when a client should offer the video-call option for a thread.
 */
class GetNextAppointmentUseCase(private val repository: AppointmentRepository) {
    suspend operator fun invoke(doctorId: String, patientId: String): DefaultResponse<NextAppointmentRes?> =
        repository.getNextConfirmedAppointment(doctorId, patientId)
            .toDefaultResponse { it?.toModel()?.toNextAppointmentRes() }
}
