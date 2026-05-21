package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.PractitionerLicence
import kotlinx.serialization.Serializable

data class PractitionerLicenceRes(
    val id: String,
    val doctorId: String,
    val licenceName: String,
    val licenceUrl: String?,
    val createdAt: String,
    val updatedAt: String?,
)

data class LicencePageResult(
    val items: List<PractitionerLicenceRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun PractitionerLicence.toRes() = PractitionerLicenceRes(
    id = id,
    doctorId = doctorId,
    licenceName = licenceName,
    licenceUrl = licenceUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)