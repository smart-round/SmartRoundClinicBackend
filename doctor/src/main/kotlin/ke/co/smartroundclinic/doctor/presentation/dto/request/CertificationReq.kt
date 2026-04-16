package ke.co.smartroundclinic.doctor.presentation.dto.request

import ke.co.smartroundclinic.doctor.domain.model.PractitionerCertification
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

data class AddCertificationReq(
    val certificationName: String,
    val certificationDate: String,
) {
    fun toModel(doctorId: String) = PractitionerCertification(
        id = ObjectId().toString(),
        doctorId = doctorId,
        certificationName = certificationName,
        certificationDate = certificationDate,
        certificationUrl = null,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}
data class UpdateCertificationReq(
    val certificationName: String? = null,
    val certificationDate: String? = null,
)
