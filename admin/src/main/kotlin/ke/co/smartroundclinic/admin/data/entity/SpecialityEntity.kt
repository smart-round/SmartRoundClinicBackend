package ke.co.smartroundclinic.admin.data.entity

import ke.co.smartroundclinic.admin.domain.model.Speciality
import ke.co.smartroundclinic.admin.domain.model.Subspecialty
import org.bson.types.ObjectId

data class SpecialityEntity(
    val id: String = ObjectId().toString(),
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
) {
    fun toModel() = Speciality(
        id = id,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
    )
}

data class SubspecialtyEntity(
    val id: String = ObjectId().toString(),
    val specialityId: String,
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
) {
    fun toModel() = Subspecialty(
        id = id,
        specialityId = specialityId,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
    )
}

