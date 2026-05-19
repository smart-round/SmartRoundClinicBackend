package ke.co.smartroundclinic.scheduling.domain.usecase

import ke.co.smartroundclinic.scheduling.domain.model.DoctorSchedule
import ke.co.smartroundclinic.scheduling.domain.model.SlotOverride

object SlotEngine {

    fun toMinutes(time: String): Int {
        val (h, m) = time.split(":").map { it.toInt() }
        return h * 60 + m
    }

    fun fromMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    // Half-open interval: slot overlaps range if slotStart < rangeEnd && (slotStart + duration) > rangeStart
    fun overlaps(slotStart: Int, duration: Int, rangeStart: Int, rangeEnd: Int): Boolean =
        slotStart < rangeEnd && (slotStart + duration) > rangeStart

    fun generateSlots(windowStart: Int, windowEnd: Int, duration: Int): List<Int> {
        val slots = mutableListOf<Int>()
        var cursor = windowStart
        while (cursor + duration <= windowEnd) {
            slots.add(cursor)
            cursor += duration
        }
        return slots
    }

    /**
     * Computes available slot start times for a given doctor schedule and date.
     *
     * @param schedule              Doctor's schedule for the day of week; slotDuration is the grace period (prep time)
     * @param consultationDuration  Actual consultation length in minutes (from ServiceTier)
     * @param bookedIntervals       Pairs of (slotStartMinutes, slotEndMinutes) for active bookings
     * @param overrides             All SlotOverrides for the specific date
     * @param nowMinutes            Current time as minutes-since-midnight (doctor's timezone); null if not today
     * @param gridStep              Step between candidate slot starts; defaults to consultationDuration + slotDuration.
     *                              Pass schedule.slotDuration to align with the calendar display grid.
     * @return Sorted list of available slot start times in "HH:mm" format
     */
    fun computeAvailableSlots(
        schedule: DoctorSchedule,
        consultationDuration: Int,
        bookedIntervals: List<Pair<Int, Int>>,
        overrides: List<SlotOverride>,
        nowMinutes: Int? = null,
        gridStep: Int? = null,
    ): List<String> {
        if (!schedule.isActive) return emptyList()

        val gracePeriod = schedule.slotDuration
        val step = gridStep ?: (consultationDuration + gracePeriod)
        val winStart = toMinutes(schedule.windowStart)
        val winEnd = toMinutes(schedule.windowEnd)

        val blocked = overrides.filter { it.type == "BLOCKED" }
            .map { toMinutes(it.start) to toMinutes(it.end) }
        val extra = overrides.filter { it.type == "EXTRA_AVAILABLE" }
            .map { toMinutes(it.start) to toMinutes(it.end) }
        val breaks = schedule.breakBlocks.map { toMinutes(it.start) to toMinutes(it.end) }

        val candidates = mutableListOf<Int>()
        candidates.addAll(generateSlots(winStart, winEnd, step))
        for ((extraStart, extraEnd) in extra) {
            candidates.addAll(generateSlots(extraStart, extraEnd, step))
        }

        return candidates
            .filter { slotStart ->
                // bookedIntervals be = bookedSlotStart + consultationDuration + gracePeriod (full blocked window)
                if (bookedIntervals.any { (bs, be) -> slotStart < be && (slotStart + consultationDuration) > bs }) return@filter false
                if (breaks.any { (bs, be) -> overlaps(slotStart, consultationDuration, bs, be) }) return@filter false
                if (blocked.any { (bs, be) -> overlaps(slotStart, consultationDuration, bs, be) }) return@filter false
                if (nowMinutes != null && slotStart < nowMinutes + 5) return@filter false
                true
            }
            .distinct()
            .sorted()
            .map { fromMinutes(it) }
    }
}
