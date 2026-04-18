package ke.co.smartroundclinic.scheduling.data.entity

import ke.co.smartroundclinic.scheduling.domain.model.SlotOverride
import kotlinx.serialization.Serializable

@Serializable
data class SlotOverrideEntity(
    val id: String,
    val doctorId: String,
    val date: String,
    val type: String,
    val start: String,
    val end: String,
    val reason: String? = null,
    val createdAt: String,
) {
    fun toModel() = SlotOverride(
        id = id,
        doctorId = doctorId,
        date = date,
        type = type,
        start = start,
        end = end,
        reason = reason,
        createdAt = createdAt,
    )
}

fun SlotOverride.toEntity() = SlotOverrideEntity(
    id = id,
    doctorId = doctorId,
    date = date,
    type = type,
    start = start,
    end = end,
    reason = reason,
    createdAt = createdAt,
)
