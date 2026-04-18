package ke.co.smartroundclinic.admin.presentation.dto.response

import ke.co.smartroundclinic.admin.domain.model.Speciality
import ke.co.smartroundclinic.admin.domain.model.Subspecialty

data class SpecialityRes(
    val id: String,
    val serviceTierId: String?,
    val title: String,
    val description: String,
    val color: String,
    val iconUrl: String?,
)

data class SubSpecialityRes(
    val id: String,
    val specialityId: String,
    val title: String,
    val description: String,
    val color: String,
    val iconUrl: String?,
)

fun Speciality.toRes() = SpecialityRes(
    id = id,
    serviceTierId = serviceTierId,
    title = title,
    description = description,
    color = color,
    iconUrl = iconUrl,
)

fun Subspecialty.toRes() = SubSpecialityRes(
    id = id,
    specialityId = specialityId,
    title = title,
    description = description,
    color = color,
    iconUrl = iconUrl,
)
