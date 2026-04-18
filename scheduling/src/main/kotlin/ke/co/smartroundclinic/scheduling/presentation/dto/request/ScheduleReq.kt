package ke.co.smartroundclinic.scheduling.presentation.dto.request

import ke.co.smartroundclinic.scheduling.domain.model.BreakBlock
import ke.co.smartroundclinic.scheduling.domain.model.DoctorSchedule
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class BreakBlockReq(val start: String, val end: String)

@Serializable
data class UpsertScheduleReq(
    val dayOfWeek: Int,
    val windowStart: String,
    val windowEnd: String,
    val slotDuration: Int,
    val breakBlocks: List<BreakBlockReq> = emptyList(),
    val isActive: Boolean = true,
    val timezone: String = "Africa/Nairobi",
) {
    fun toModel(doctorId: String) = DoctorSchedule(
        id = ObjectId().toString(),
        doctorId = doctorId,
        dayOfWeek = dayOfWeek,
        windowStart = windowStart,
        windowEnd = windowEnd,
        slotDuration = slotDuration,
        breakBlocks = breakBlocks.map { BreakBlock(it.start, it.end) },
        isActive = isActive,
        timezone = timezone,
        createdAt = Clock.System.now().toString(),
    )
}

@Serializable
data class UpdateScheduleDayReq(
    val windowStart: String? = null,
    val windowEnd: String? = null,
    val slotDuration: Int? = null,
    val breakBlocks: List<BreakBlockReq>? = null,
    val isActive: Boolean? = null,
    val timezone: String? = null,
)
