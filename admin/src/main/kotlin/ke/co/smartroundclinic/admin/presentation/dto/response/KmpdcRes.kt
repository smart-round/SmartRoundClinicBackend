package ke.co.smartroundclinic.admin.presentation.dto.response

import ke.co.smartroundclinic.admin.data.entity.KmpdcPractitionerEntity
import kotlinx.serialization.Serializable

@Serializable
data class KmpdcPractitionerRes(
    val id: String,
    val fullName: String,
    val regNumber: String,
    val address: String,
    val qualifications: String,
    val status: String,
    val practitionerType: String,
    val registerType: String,
    val speciality: String?,
    val subSpeciality: String?,
    val licenceType: String?,
    val discipline: String?,
    val specialty: String?,
    val lastRefreshedAt: Long,
)

@Serializable
data class KmpdcPageResult(
    val items: List<KmpdcPractitionerRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

@Serializable
data class KmpdcRefreshRes(
    val newRecords: Int,
    val totalScraped: Int,
    val failedRegisters: List<String>,
)

fun KmpdcPractitionerEntity.toRes() = KmpdcPractitionerRes(
    id = id,
    fullName = fullName,
    regNumber = regNumber,
    address = address,
    qualifications = qualifications,
    status = status,
    practitionerType = practitionerType,
    registerType = registerType,
    speciality = speciality,
    subSpeciality = subSpeciality,
    licenceType = licenceType,
    discipline = discipline,
    specialty = specialty,
    lastRefreshedAt = lastRefreshedAt,
)
