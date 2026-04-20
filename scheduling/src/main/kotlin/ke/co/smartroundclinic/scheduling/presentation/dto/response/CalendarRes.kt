package ke.co.smartroundclinic.scheduling.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class SlotEntryRes(
    val slotStart: String,
    val slotEnd: String,
    val status: String,         // AVAILABLE | BOOKED | CONFIRMED | BLOCKED
    val appointmentId: String? = null,
    val patientId: String? = null,
)

@Serializable
data class DayViewRes(
    val date: String,
    val dayOfWeek: String,
    val isWorkingDay: Boolean,
    val slots: List<SlotEntryRes>,
)

@Serializable
data class CalendarRangeRes(
    val doctorId: String,
    val days: List<DayViewRes>,
)
