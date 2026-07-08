package ke.co.smartroundclinic.patient.domain.model

import ke.co.smartroundclinic.patient.data.entity.PatientRatingEntity

data class PatientRating(
    val id: String,
    val appointmentId: String,
    val doctorId: String,
    val patientId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val updatedAt: String?,
)

fun PatientRatingEntity.toModel() = PatientRating(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    patientId = patientId,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PatientRating.toEntity() = PatientRatingEntity(
    id = id,
    appointmentId = appointmentId,
    doctorId = doctorId,
    patientId = patientId,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
