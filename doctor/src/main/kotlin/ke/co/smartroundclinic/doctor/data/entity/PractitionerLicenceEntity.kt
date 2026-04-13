package ke.co.smartroundclinic.doctor.data.entity

import kotlin.time.Clock

data class PractitionerLicenceEntity(
    val id: String,
    val complianceId: String,
    val doctorId: String,
    val licenceUrl: String,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)

enum class LicenceCategory{

}
