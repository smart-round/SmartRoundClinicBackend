package ke.co.smartroundclinic.scheduling.domain.usecase

import ke.co.smartroundclinic.scheduling.domain.model.BreakBlock
import ke.co.smartroundclinic.scheduling.domain.model.DoctorSchedule
import ke.co.smartroundclinic.scheduling.domain.model.SlotOverride
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlotEngineTest {

    private fun schedule(
        windowStart: String = "09:00",
        windowEnd: String = "17:00",
        slotDuration: Int = 30,
        breakBlocks: List<BreakBlock> = emptyList(),
        isActive: Boolean = true,
        timezone: String = "Africa/Nairobi",
    ) = DoctorSchedule(
        id = "doc-1",
        doctorId = "doc-1",
        dayOfWeek = 0,
        windowStart = windowStart,
        windowEnd = windowEnd,
        slotDuration = slotDuration,
        breakBlocks = breakBlocks,
        isActive = isActive,
        timezone = timezone,
        createdAt = "2025-01-01T00:00:00Z",
    )

    private fun override(type: String, start: String, end: String) = SlotOverride(
        id = "ov-1",
        doctorId = "doc-1",
        date = "2025-07-14",
        type = type,
        start = start,
        end = end,
        createdAt = "2025-01-01T00:00:00Z",
    )

    // --- Basic generation ---

    // slotDuration = gracePeriod, consultationDuration = 25 → totalDuration = 30
    // The window 09:00–10:00 with totalDuration=30 produces 09:00, 09:30
    @Test
    fun `generates slots across full window`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "10:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(), overrides = emptyList()
        )
        assertEquals(listOf("09:00", "09:30"), slots)
    }

    @Test
    fun `returns empty list when inactive`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(isActive = false), consultationDuration = 25,
            bookedIntervals = emptyList(), overrides = emptyList()
        )
        assertTrue(slots.isEmpty())
    }

    @Test
    fun `window too short for any slot returns empty`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "09:20", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(), overrides = emptyList()
        )
        assertTrue(slots.isEmpty())
    }

    @Test
    fun `window exactly one total duration produces single slot`() {
        // consultationDuration=25, gracePeriod=5 → totalDuration=30; window 09:00–09:30 → exactly one slot
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "09:30", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(), overrides = emptyList()
        )
        assertEquals(listOf("09:00"), slots)
    }

    // --- Already-booked filtering ---

    @Test
    fun `booked slot interval is excluded`() {
        // Booked: 09:00–09:25 (consultationDuration=25). Next slot 09:30 is clear.
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "10:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = listOf(SlotEngine.toMinutes("09:00") to SlotEngine.toMinutes("09:25")),
            overrides = emptyList()
        )
        assertEquals(listOf("09:30"), slots)
    }

    // --- Break block filtering ---

    @Test
    fun `slot overlapping break block is excluded`() {
        // consultationDuration=25, gracePeriod=5 → totalDuration=30; break 10:00–11:00
        val slots = SlotEngine.computeAvailableSlots(
            schedule(
                windowStart = "09:00",
                windowEnd = "12:00",
                slotDuration = 5,
                breakBlocks = listOf(BreakBlock("10:00", "11:00"))
            ),
            consultationDuration = 25,
            bookedIntervals = emptyList(), overrides = emptyList()
        )
        assertFalse("10:00" in slots)
        assertFalse("10:30" in slots)
        assertTrue("09:00" in slots)
        assertTrue("09:30" in slots)
        assertTrue("11:00" in slots)
    }

    @Test
    fun `slot ending exactly at break start is NOT excluded (half-open interval)`() {
        // consultationDuration=25, gracePeriod=5 → totalDuration=30
        // Slot 09:30 ends at 09:55 (consultation), well before break at 10:00 → included
        // Slot 10:00 starts at break → excluded
        val slots = SlotEngine.computeAvailableSlots(
            schedule(
                windowStart = "09:00",
                windowEnd = "11:00",
                slotDuration = 5,
                breakBlocks = listOf(BreakBlock("10:00", "11:00"))
            ),
            consultationDuration = 25,
            bookedIntervals = emptyList(), overrides = emptyList()
        )
        assertTrue("09:30" in slots, "Slot ending before break start should be included")
        assertFalse("10:00" in slots, "Slot starting at break start should be excluded")
    }

    // --- BLOCKED override ---

    @Test
    fun `blocked override removes slots in range`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "12:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(),
            overrides = listOf(override("BLOCKED", "10:00", "11:00"))
        )
        assertFalse("10:00" in slots)
        assertFalse("10:30" in slots)
        assertTrue("09:30" in slots)
        assertTrue("11:00" in slots)
    }

    @Test
    fun `full-day block returns empty slots`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "17:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(),
            overrides = listOf(override("BLOCKED", "00:00", "23:59"))
        )
        assertTrue(slots.isEmpty())
    }

    // --- EXTRA_AVAILABLE override ---

    @Test
    fun `extra-available override adds slots outside main window`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "12:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(),
            overrides = listOf(override("EXTRA_AVAILABLE", "17:00", "18:00"))
        )
        assertTrue("17:00" in slots)
        assertTrue("17:30" in slots)
    }

    @Test
    fun `extra-available slot is blocked if in blocked range`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "12:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(),
            overrides = listOf(
                override("EXTRA_AVAILABLE", "17:00", "18:00"),
                override("BLOCKED", "17:00", "17:30"),
            )
        )
        assertFalse("17:00" in slots)
        assertTrue("17:30" in slots)
    }

    // --- Same-day lead-time filtering ---

    @Test
    fun `slots before now plus 5 minutes are excluded on same day`() {
        // now = 09:20, so slots < 09:25 are excluded (09:00 excluded, 09:30 included)
        val nowMinutes = 9 * 60 + 20
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "10:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(), overrides = emptyList(),
            nowMinutes = nowMinutes
        )
        assertFalse("09:00" in slots)
        assertTrue("09:30" in slots)
    }

    @Test
    fun `all slots included when date is not today (nowMinutes is null)`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "10:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(), overrides = emptyList(),
            nowMinutes = null
        )
        assertEquals(listOf("09:00", "09:30"), slots)
    }

    // --- toMinutes / fromMinutes helpers ---

    @Test
    fun `toMinutes parses HH_mm correctly`() {
        assertEquals(0, SlotEngine.toMinutes("00:00"))
        assertEquals(60, SlotEngine.toMinutes("01:00"))
        assertEquals(545, SlotEngine.toMinutes("09:05"))
        assertEquals(1439, SlotEngine.toMinutes("23:59"))
    }

    @Test
    fun `fromMinutes formats correctly`() {
        assertEquals("00:00", SlotEngine.fromMinutes(0))
        assertEquals("09:00", SlotEngine.fromMinutes(540))
        assertEquals("23:59", SlotEngine.fromMinutes(1439))
        assertEquals("09:25", SlotEngine.fromMinutes(565))
    }

    // --- Custom consultation duration ---

    @Test
    fun `20-minute consultation with 5-minute grace generates correct slots`() {
        // totalDuration=25; window 09:00–10:15 → 09:00, 09:25, 09:50 (09:50+25=10:15 ≤ 10:15)
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "10:15", slotDuration = 5),
            consultationDuration = 20,
            bookedIntervals = emptyList(), overrides = emptyList()
        )
        assertEquals(listOf("09:00", "09:25", "09:50"), slots)
    }

    // --- Deduplication ---

    @Test
    fun `duplicate candidates from overlapping extra ranges are deduplicated`() {
        val slots = SlotEngine.computeAvailableSlots(
            schedule(windowStart = "09:00", windowEnd = "10:00", slotDuration = 5),
            consultationDuration = 25,
            bookedIntervals = emptyList(),
            overrides = listOf(override("EXTRA_AVAILABLE", "09:00", "10:00"))
        )
        assertEquals(slots, slots.distinct())
    }
}
