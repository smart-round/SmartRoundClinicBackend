package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.DoctorRating
import kotlinx.serialization.Serializable

@Serializable
data class RatingRes(
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
data class RatingsPageRes(
    val items: List<RatingRes>,
    val total: Long,
    val page: Int,
    val size: Int,
)

fun DoctorRating.toRes() = RatingRes(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    patientId = patientId,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
