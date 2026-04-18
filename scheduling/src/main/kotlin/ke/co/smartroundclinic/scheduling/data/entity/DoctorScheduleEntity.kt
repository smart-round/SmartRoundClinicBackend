package ke.co.smartroundclinic.scheduling.data.entity

import ke.co.smartroundclinic.scheduling.domain.model.BreakBlock
import ke.co.smartroundclinic.scheduling.domain.model.DoctorSchedule
import kotlinx.serialization.Serializable

@Serializable
data class BreakBlockDoc(val start: String, val end: String)

@Serializable
data class DoctorScheduleEntity(
    val id: String,
    val doctorId: String,
    val dayOfWeek: Int,
    val windowStart: String,
    val windowEnd: String,
    val slotDuration: Int,
    val breakBlocks: List<BreakBlockDoc>,
    val isActive: Boolean,
    val timezone: String,
    val createdAt: String,
    val updatedAt: String? = null,
) {
    fun toModel() = DoctorSchedule(
        id = id,
        doctorId = doctorId,
        dayOfWeek = dayOfWeek,
        windowStart = windowStart,
        windowEnd = windowEnd,
        slotDuration = slotDuration,
        breakBlocks = breakBlocks.map { BreakBlock(it.start, it.end) },
        isActive = isActive,
        timezone = timezone,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun DoctorSchedule.toEntity() = DoctorScheduleEntity(
    id = id,
    doctorId = doctorId,
    dayOfWeek = dayOfWeek,
    windowStart = windowStart,
    windowEnd = windowEnd,
    slotDuration = slotDuration,
    breakBlocks = breakBlocks.map { BreakBlockDoc(it.start, it.end) },
    isActive = isActive,
    timezone = timezone,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
