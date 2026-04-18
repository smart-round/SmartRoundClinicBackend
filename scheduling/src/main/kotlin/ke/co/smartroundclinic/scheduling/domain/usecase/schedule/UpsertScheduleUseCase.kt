package ke.co.smartroundclinic.scheduling.domain.usecase.schedule

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.data.entity.toEntity
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.request.UpsertScheduleReq
import ke.co.smartroundclinic.scheduling.presentation.dto.response.DoctorScheduleRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class UpsertScheduleUseCase(private val repository: DoctorScheduleRepository) {
    suspend operator fun invoke(req: UpsertScheduleReq, doctorId: String): DefaultResponse<DoctorScheduleRes?> =
        repository.upsert(req.toModel(doctorId).toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 400) { it?.toModel()?.toRes() }
}
