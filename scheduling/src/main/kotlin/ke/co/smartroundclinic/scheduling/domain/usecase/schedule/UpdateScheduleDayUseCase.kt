package ke.co.smartroundclinic.scheduling.domain.usecase.schedule

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.scheduling.domain.model.BreakBlock
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.request.UpdateScheduleDayReq
import ke.co.smartroundclinic.scheduling.presentation.dto.response.DoctorScheduleRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class UpdateScheduleDayUseCase(private val repository: DoctorScheduleRepository) {
    suspend operator fun invoke(
        doctorId: String,
        dayOfWeek: Int,
        req: UpdateScheduleDayReq,
    ): DefaultResponse<DoctorScheduleRes?> =
        repository.update(
            doctorId = doctorId,
            dayOfWeek = dayOfWeek,
            windowStart = req.windowStart,
            windowEnd = req.windowEnd,
            slotDuration = req.slotDuration,
            breakBlocks = req.breakBlocks?.map { BreakBlock(it.start, it.end) },
            isActive = req.isActive,
            timezone = req.timezone,
        ).toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
}
