package ke.co.smartroundclinic.doctor.presentation.dto.request

import ke.co.smartroundclinic.doctor.domain.model.DoctorRating
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class SubmitRatingReq(
    val appointmentId: String,
    val doctorId: String,
    val rating: Int,
    val comment: String? = null,
) {
    fun toModel(patientId: String) = DoctorRating(
        id = ObjectId().toString(),
        appointmentId = appointmentId,
        doctorId = doctorId,
        patientId = patientId,
        rating = rating,
        comment = comment,
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}

@Serializable
data class UpdateRatingReq(
    val rating: Int? = null,
    val comment: String? = null,
)
