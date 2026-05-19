package ke.co.smartroundclinic.scheduling.domain.service

import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.DeactivateDayUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.GetAvailableSlotsUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.GetScheduleUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.UpdateScheduleDayUseCase
import ke.co.smartroundclinic.scheduling.domain.usecase.schedule.UpsertScheduleUseCase
import ke.co.smartroundclinic.scheduling.presentation.dto.request.UpdateScheduleDayReq
import ke.co.smartroundclinic.scheduling.presentation.dto.request.UpsertScheduleReq

class ScheduleService(
    private val upsertScheduleUseCase: UpsertScheduleUseCase,
    private val getScheduleUseCase: GetScheduleUseCase,
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase,
    private val updateScheduleDayUseCase: UpdateScheduleDayUseCase,
    private val deactivateDayUseCase: DeactivateDayUseCase,
) {
    suspend fun upsert(req: UpsertScheduleReq, doctorId: String) = upsertScheduleUseCase(req, doctorId)
    suspend fun getSchedule(doctorId: String) = getScheduleUseCase(doctorId)
    suspend fun getAvailableSlots(doctorId: String, date: String) =
        getAvailableSlotsUseCase(doctorId, date)
    suspend fun updateDay(doctorId: String, day: Int, req: UpdateScheduleDayReq) =
        updateScheduleDayUseCase(doctorId, day, req)
    suspend fun deactivateDay(doctorId: String, day: Int) = deactivateDayUseCase(doctorId, day)
}
