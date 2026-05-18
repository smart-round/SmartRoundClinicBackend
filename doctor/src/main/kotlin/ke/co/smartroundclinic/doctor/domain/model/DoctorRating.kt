package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.DoctorRatingEntity

data class DoctorRating(
    val id: String,
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val updatedAt: String?,
)

fun DoctorRatingEntity.toModel() = DoctorRating(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    patientId = patientId,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun DoctorRating.toEntity() = DoctorRatingEntity(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    patientId = patientId,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
