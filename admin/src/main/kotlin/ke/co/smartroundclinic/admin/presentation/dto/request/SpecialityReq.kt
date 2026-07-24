package ke.co.smartroundclinic.admin.presentation.dto.request

import ke.co.smartroundclinic.admin.domain.model.Speciality
import ke.co.smartroundclinic.admin.domain.model.Subspecialty
import org.bson.types.ObjectId
import kotlin.time.Clock

data class CreateSpecialityReq(
    val serviceTierId: String? = null,
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
) {
    fun toModel(iconUrl: String? = null) = Speciality(
        id = ObjectId().toString(),
        serviceTierId = serviceTierId,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
        createdAt = Clock.System.now().toString(),
    )
}

data class UpdateSpecialityReq(
    val serviceTierId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val color: String? = null,
)

data class CreateSubSpecialityReq(
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
) {
    fun toModel(specialityId: String, iconUrl: String? = null) = Subspecialty(
        id = ObjectId().toString(),
        specialityId = specialityId,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
    )
}

data class UpdateSubSpecialityReq(
    val title: String? = null,
    val description: String? = null,
    val color: String? = null,
)
