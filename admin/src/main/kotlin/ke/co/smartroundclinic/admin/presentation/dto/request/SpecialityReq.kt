package ke.co.smartroundclinic.admin.presentation.dto.request

import ke.co.smartroundclinic.admin.domain.model.Speciality
import ke.co.smartroundclinic.admin.domain.model.Subspecialty
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class CreateSpecialityReq(
    val serviceTierId: String? = null,
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
) {
    fun toModel() = Speciality(
        id = ObjectId().toString(),
        serviceTierId = serviceTierId,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
    )
}

@Serializable
data class UpdateSpecialityReq(
    val serviceTierId: String? = null,
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
    fun toModel(specialityId: String) = Subspecialty(
        id = ObjectId().toString(),
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
