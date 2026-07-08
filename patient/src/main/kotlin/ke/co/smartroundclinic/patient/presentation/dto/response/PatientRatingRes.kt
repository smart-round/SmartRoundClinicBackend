package ke.co.smartroundclinic.patient.presentation.dto.response

import ke.co.smartroundclinic.patient.domain.model.PatientRating
import kotlinx.serialization.Serializable

@Serializable
data class PatientRatingRes(
    val id: String,
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class PatientRatingsPageRes(
    val items: List<PatientRatingRes>,
    val total: Long,
    val page: Int,
    val size: Int,
)

fun PatientRating.toRes() = PatientRatingRes(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    patientId = patientId,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
