package ke.co.smartroundclinic.admin.data.entity

import ke.co.smartroundclinic.admin.domain.model.Speciality
import ke.co.smartroundclinic.admin.domain.model.Subspecialty
import kotlin.time.Clock
import org.bson.types.ObjectId

data class SpecialityEntity(
    val id: String = ObjectId().toString(),
    val serviceTierId: String? = null,
    val serviceCategoryId: String? = null,
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = Speciality(
        id = id,
        serviceTierId = serviceTierId,
        serviceCategoryId = serviceCategoryId,
        title = title,
        description = description,
        color = color,
        iconUrl = iconUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
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

fun Speciality.toEntity() = SpecialityEntity(
    id = id,
    serviceTierId = serviceTierId,
    serviceCategoryId = serviceCategoryId,
    title = title,
    description = description,
    color = color,
    iconUrl = iconUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Subspecialty.toEntity() = SubspecialtyEntity(
    id = id,
    specialityId = specialityId,
    title = title,
    description = description,
    color = color,
    iconUrl = iconUrl,
)
