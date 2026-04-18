package ke.co.smartroundclinic.scheduling.presentation.dto.response

import ke.co.smartroundclinic.scheduling.domain.model.DoctorSchedule
import kotlinx.serialization.Serializable

@Serializable
data class BreakBlockRes(val start: String, val end: String)

@Serializable
data class DoctorScheduleRes(
    val id: String,
    val doctorId: String,
    val dayOfWeek: Int,
    val windowStart: String,
    val windowEnd: String,
    val slotDuration: Int,
    val breakBlocks: List<BreakBlockRes>,
    val isActive: Boolean,
    val timezone: String,
    val createdAt: String,
    val updatedAt: String?,
)

fun DoctorSchedule.toRes() = DoctorScheduleRes(
    id = id,
    doctorId = doctorId,
    dayOfWeek = dayOfWeek,
    windowStart = windowStart,
    windowEnd = windowEnd,
    slotDuration = slotDuration,
    breakBlocks = breakBlocks.map { BreakBlockRes(it.start, it.end) },
    isActive = isActive,
    timezone = timezone,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
