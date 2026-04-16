package ke.co.smartroundclinic.doctor.presentation.dto.request

import ke.co.smartroundclinic.doctor.domain.model.PractitionerLicence
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock


data class AddLicenceReq(
    val licenceName: String,
) {
    fun toModel(doctorId: String) = PractitionerLicence(
        id = ObjectId().toString(),
        doctorId = doctorId,
        licenceName = licenceName,
        licenceUrl = null,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}

@Serializable
data class UpdateLicenceReq(
    val licenceName: String? = null,
)