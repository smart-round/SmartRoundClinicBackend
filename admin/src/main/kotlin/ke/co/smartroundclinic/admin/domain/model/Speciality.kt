package ke.co.smartroundclinic.admin.domain.model

import ke.co.smartroundclinic.admin.presentation.dto.response.SpecialityRes
import ke.co.smartroundclinic.admin.presentation.dto.response.SubSpecialityRes
import org.bson.types.ObjectId

data class Speciality(
    val id: String = ObjectId().toString(),
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
) {
    fun toRes() = SpecialityRes(
        id = id,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
    )
}

data class Subspecialty(
    val id: String,
    val specialityId: String,
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
) {
    fun toRes() = SubSpecialityRes(
        id = id,
        specialityId = specialityId,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
    )
}
