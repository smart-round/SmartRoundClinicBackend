package ke.co.smartroundclinic.scheduling.domain.usecase.schedule

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.domain.repository.DoctorScheduleRepository
import ke.co.smartroundclinic.scheduling.domain.repository.SlotOverrideRepository
import ke.co.smartroundclinic.scheduling.domain.usecase.SlotEngine
import ke.co.smartroundclinic.scheduling.presentation.dto.response.CalendarRangeRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.DayViewRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.SlotEntryRes
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class GetCalendarRangeUseCase(
    private val scheduleRepository: DoctorScheduleRepository,
    private val appointmentRepository: AppointmentRepository,
    private val overrideRepository: SlotOverrideRepository,
) {
    // forDoctor=true → show all slots with statuses; false → only AVAILABLE slots (patient view)
    suspend operator fun invoke(
        doctorId: String,
        from: LocalDate,
        to: LocalDate,
        forDoctor: Boolean,
    ): DefaultResponse<CalendarRangeRes?> {
        val allSchedules = (scheduleRepository.getByDoctor(doctorId) as? Resource.Success)
            ?.data?.associateBy { it.dayOfWeek } ?: emptyMap()

        val fromStr = from.toString()
        val toStr = to.toString()

        val allAppointments = (appointmentRepository.getByDoctorAndDateRange(doctorId, fromStr, toStr) as? Resource.Success)
            ?.data ?: emptyList()

        val allOverrides = (overrideRepository.getByDoctorAndDateRange(doctorId, fromStr, toStr) as? Resource.Success)
            ?.data ?: emptyList()

        val appointmentsByDate = allAppointments.groupBy { it.date }
        val overridesByDate = allOverrides.groupBy { it.date }

        val days = mutableListOf<DayViewRes>()
        var cursor = from
        while (cursor <= to) {
            val dateStr = cursor.toString()
            val dow = cursor.dayOfWeek.ordinal  // 0=Mon..6=Sun
            val scheduleEntity = allSchedules[dow]

            if (scheduleEntity == null || !scheduleEntity.isActive) {
                days.add(DayViewRes(date = dateStr, dayOfWeek = cursor.dayOfWeek.name, isWorkingDay = false, slots = emptyList()))
                cursor = cursor.plus(DatePeriod(days = 1))
                continue
            }

            val schedule = scheduleEntity.toModel()
            val dayAppointments = appointmentsByDate[dateStr] ?: emptyList()
            val dayOverrides = (overridesByDate[dateStr] ?: emptyList()).map { it.toModel() }

            // Determine nowMinutes only when computing for today
            val tz = TimeZone.of(schedule.timezone)
            val nowLdt = Clock.System.now().toLocalDateTime(tz)
            val today = nowLdt.date.toString()
            val nowMinutes = if (dateStr == today) nowLdt.hour * 60 + nowLdt.minute else null

            val duration = schedule.slotDuration
            val winStart = SlotEngine.toMinutes(schedule.windowStart)
            val winEnd = SlotEngine.toMinutes(schedule.windowEnd)
            val breaks = schedule.breakBlocks.map { SlotEngine.toMinutes(it.start) to SlotEngine.toMinutes(it.end) }
            val blocked = dayOverrides.filter { it.type == "BLOCKED" }
                .map { SlotEngine.toMinutes(it.start) to SlotEngine.toMinutes(it.end) }
            val extra = dayOverrides.filter { it.type == "EXTRA_AVAILABLE" }
                .map { SlotEngine.toMinutes(it.start) to SlotEngine.toMinutes(it.end) }

            val allSlotStarts = mutableListOf<Int>()
            allSlotStarts.addAll(SlotEngine.generateSlots(winStart, winEnd, duration))
            for ((es, ee) in extra) allSlotStarts.addAll(SlotEngine.generateSlots(es, ee, duration))
            allSlotStarts.sort()

            // Map slotStart → appointment for quick lookup
            val bookedMap = dayAppointments.associateBy { it.slotStart }

            val slots = allSlotStarts.distinct().mapNotNull { slotMinutes ->
                val slotStart = SlotEngine.fromMinutes(slotMinutes)
                val slotEnd = SlotEngine.fromMinutes(slotMinutes + duration)

                val isPast = nowMinutes != null && slotMinutes < nowMinutes + 5
                val isBreak = breaks.any { (bs, be) -> SlotEngine.overlaps(slotMinutes, duration, bs, be) }
                val isBlocked = blocked.any { (bs, be) -> SlotEngine.overlaps(slotMinutes, duration, bs, be) }

                val appointment = bookedMap[slotStart]
                val status = when {
                    isBlocked -> "BLOCKED"
                    isBreak -> "BLOCKED"
                    isPast -> null  // skip past slots entirely
                    appointment != null -> appointment.status  // BOOKED | CONFIRMED | CANCELLED | etc.
                    else -> "AVAILABLE"
                }

                if (status == null) return@mapNotNull null
                if (!forDoctor && status != "AVAILABLE") return@mapNotNull null

                SlotEntryRes(
                    slotStart = slotStart,
                    slotEnd = slotEnd,
                    status = status,
                    appointmentId = if (forDoctor) appointment?.id else null,
                    patientId = if (forDoctor) appointment?.patientId else null,
                )
            }

            days.add(DayViewRes(date = dateStr, dayOfWeek = cursor.dayOfWeek.name, isWorkingDay = true, slots = slots))
            cursor = cursor.plus(DatePeriod(days = 1))
        }

        return Resource.Success(
            data = CalendarRangeRes(doctorId = doctorId, days = days),
            message = "Calendar retrieved successfully",
        ).toDefaultResponse { it }
    }
}
