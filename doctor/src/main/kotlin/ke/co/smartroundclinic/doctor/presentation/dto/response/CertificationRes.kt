package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.PractitionerCertification
import kotlinx.serialization.Serializable

@Serializable
data class CertificationRes(
    val id: String,
    val doctorId: String,
    val certificationName: String,
    val certificationDate: String,
    val certificationUrl: String?,
    val createdAt: String,
    val updatedAt: String?,
)

fun PractitionerCertification.toRes() = CertificationRes(
    id = id,
    doctorId = doctorId,
    certificationName = certificationName,
    certificationDate = certificationDate,
    certificationUrl = certificationUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)