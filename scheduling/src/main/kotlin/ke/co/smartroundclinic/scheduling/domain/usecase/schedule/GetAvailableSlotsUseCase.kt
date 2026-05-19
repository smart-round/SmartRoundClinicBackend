package ke.co.smartroundclinic.scheduling.domain.usecase.schedule

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.repository.ConsultationInfoResult
import ke.co.smartroundclinic.scheduling.data.repository.ServiceTierLookup
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.domain.repository.SlotOverrideRepository
import ke.co.smartroundclinic.scheduling.domain.usecase.SlotEngine
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetAvailableSlotsUseCase(
    private val scheduleRepository: DoctorScheduleRepository,
    private val appointmentRepository: AppointmentRepository,
    private val overrideRepository: SlotOverrideRepository,
    private val serviceTierLookup: ServiceTierLookup,
) {
    suspend operator fun invoke(doctorId: String, date: String): DefaultResponse<List<String>?> {
        val tierInfo = serviceTierLookup.getConsultationInfoForDoctor(doctorId)
        if (tierInfo is ConsultationInfoResult.Error) {
            return Resource.Error<Nothing>(tierInfo.message)
                .toDefaultResponse(failedStatusCode = tierInfo.httpStatus) { null }
        }
        val consultationDuration = (tierInfo as ConsultationInfoResult.Success).consultationDuration

        val localDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            return Resource.Error<Nothing>("Invalid date format, expected YYYY-MM-DD")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }
        val dayOfWeek = localDate.dayOfWeek.ordinal

        val scheduleResource = scheduleRepository.getByDoctorAndDay(doctorId, dayOfWeek)
        val schedule = if (scheduleResource is Resource.Success && scheduleResource.data != null) {
            scheduleResource.data!!.toModel()
        } else {
            return Resource.Success<List<String>?>(data = emptyList(), message = "No availability for this day")
                .toDefaultResponse { it }
        }

        if (!schedule.isActive) {
            return Resource.Success<List<String>?>(data = emptyList(), message = "Doctor not available on this day")
                .toDefaultResponse { it }
        }

        val bookedIntervals = (appointmentRepository.getByDoctorAndDate(doctorId, date) as? Resource.Success)
            ?.data
            ?.filter { it.status == "BOOKED" || it.status == "CONFIRMED" }
            ?.map { SlotEngine.toMinutes(it.slotStart) to SlotEngine.toMinutes(it.slotEnd) }
            ?: emptyList()

        val overrides = (overrideRepository.getByDoctorAndDate(doctorId, date) as? Resource.Success)
            ?.data
            ?.map { it.toModel() } ?: emptyList()

        val tz = TimeZone.of(schedule.timezone)
        val nowLdt = Clock.System.now().toLocalDateTime(tz)
        val today = nowLdt.date.toString()
        val nowMinutes = if (date == today) nowLdt.hour * 60 + nowLdt.minute else null

        val slots = SlotEngine.computeAvailableSlots(schedule, consultationDuration, bookedIntervals, overrides, nowMinutes, gridStep = schedule.slotDuration)
        return Resource.Success<List<String>?>(data = slots, message = "Available slots retrieved")
            .toDefaultResponse { it }
    }
}
