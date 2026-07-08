package ke.co.smartroundclinic.patient.presentation.dto.request

import ke.co.smartroundclinic.patient.domain.model.PatientRating
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class SubmitPatientRatingReq(
    val appointmentId: String,
    val patientId: String,
    val rating: Int,
    val comment: String? = null,
) {
    fun toModel(doctorId: String) = PatientRating(
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
data class UpdatePatientRatingReq(
    val rating: Int? = null,
    val comment: String? = null,
)
