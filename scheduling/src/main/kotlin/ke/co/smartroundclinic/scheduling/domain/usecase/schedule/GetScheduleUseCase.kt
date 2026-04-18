package ke.co.smartroundclinic.scheduling.domain.usecase.schedule

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.DoctorScheduleRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class GetScheduleUseCase(private val repository: DoctorScheduleRepository) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<List<DoctorScheduleRes>?> =
        repository.getByDoctor(doctorId)
            .toDefaultResponse { items -> items?.map { it.toModel().toRes() } }
}
