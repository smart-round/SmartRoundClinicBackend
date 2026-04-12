package ke.co.smartroundclinic.admin.presentation.dto.request

import ke.co.smartroundclinic.admin.data.entity.SpecialityEntity
import ke.co.smartroundclinic.admin.data.entity.SubspecialtyEntity
import kotlinx.serialization.Serializable

@Serializable
data class CreateSpecialityReq(
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
) {
    fun toEntity() = SpecialityEntity(
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
    )
}

@Serializable
data class UpdateSpecialityReq(
    val title: String? = null,
    val description: String? = null,
    val color: String? = null,
    val iconUrl: String? = null,
)

@Serializable
data class CreateSubSpecialityReq(
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
) {
    fun toEntity(specialityId: String) = SubspecialtyEntity(
        specialityId = specialityId,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
    )
}

@Serializable
data class UpdateSubSpecialityReq(
    val title: String? = null,
    val description: String? = null,
    val color: String? = null,
    val iconUrl: String? = null,
)
