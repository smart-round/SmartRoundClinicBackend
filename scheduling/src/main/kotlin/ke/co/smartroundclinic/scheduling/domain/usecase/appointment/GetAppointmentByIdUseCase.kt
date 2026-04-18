package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class GetAppointmentByIdUseCase(private val repository: AppointmentRepository) {
    suspend operator fun invoke(id: String): DefaultResponse<AppointmentRes?> =
        repository.getById(id).toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
