package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class GetDoctorAppointmentsUseCase(private val repository: AppointmentRepository) {
    suspend operator fun invoke(doctorId: String, date: String): DefaultResponse<List<AppointmentRes>?> =
        repository.getByDoctorAndDate(doctorId, date)
            .toDefaultResponse { items -> items?.map { it.toModel().toRes() } }
}
