package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.PractitionerCertificationsEntity
import org.bson.types.ObjectId

data class PractitionerCertification(
    val id: String,
    val doctorId: String,
    val certificationName: String,
    val certificationDate: String,
    val certificationUrl: String?,
    val createdAt: String,
    val updatedAt: String?,
)

fun PractitionerCertificationsEntity.toModel() = PractitionerCertification(
    id = id,
    doctorId = doctorId,
    certificationName = certificationName,
    certificationDate = certificationDate,
    certificationUrl = certificationUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PractitionerCertification.toEntity() = PractitionerCertificationsEntity(
    id = id,
    doctorId = doctorId,
    certificationName = certificationName,
    certificationDate = certificationDate,
    certificationUrl = certificationUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
