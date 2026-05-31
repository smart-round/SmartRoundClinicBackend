package ke.co.smartroundclinic.scheduling.domain.service

import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAdminAllAppointmentsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.appointment.GetAdminAppointmentStatsUseCase

class AdminAppointmentService(
    private val statsUseCase: GetAdminAppointmentStatsUseCase,
    private val listUseCase: GetAdminAllAppointmentsUseCase,
) {
    suspend fun getStats() = statsUseCase()
    suspend fun getAll(status: String?, page: Int, size: Int) = listUseCase(status, page, size)
}
