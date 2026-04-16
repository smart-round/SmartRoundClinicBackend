package ke.co.smartroundclinic.doctor.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class PractitionerLicenceEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val licenceName: String,
    val licenceUrl: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)
