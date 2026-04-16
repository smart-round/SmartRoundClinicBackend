package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.PractitionerLicenceEntity
import org.bson.types.ObjectId

data class PractitionerLicence(
    val id: String,
    val doctorId: String,
    val licenceName: String,
    val licenceUrl: String?,
    val createdAt: String,
    val updatedAt: String?,
)

fun PractitionerLicenceEntity.toModel() = PractitionerLicence(
    id = id,
    doctorId = doctorId,
    licenceName = licenceName,
    licenceUrl = licenceUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PractitionerLicence.toEntity() = PractitionerLicenceEntity(
    id = id,
    doctorId = doctorId,
    licenceName = licenceName,
    licenceUrl = licenceUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
