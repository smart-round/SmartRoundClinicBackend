package ke.co.smartroundclinic.scheduling.domain.usecase.schedule

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.DoctorScheduleRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class DeactivateDayUseCase(private val repository: DoctorScheduleRepository) {
    suspend operator fun invoke(doctorId: String, dayOfWeek: Int): DefaultResponse<DoctorScheduleRes?> =
        repository.deactivate(doctorId, dayOfWeek)
            .toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
