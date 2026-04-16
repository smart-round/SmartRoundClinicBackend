package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.PractitionerProfileEntity

data class PractitionerProfile(
    val id: String,
    val doctorId: String,
    val kmpdcRegNumber: String?,
    val title: String?,
    val bio: String?,
    val yearsOfExperience: Int?,
    val languages: List<String>,
    val facilityName: String?,
    val averageRating: Double,
    val totalReviews: Int,
    val totalConsultations: Int,
    val createdAt: String,
    val updatedAt: String?,
)

fun PractitionerProfileEntity.toModel() = PractitionerProfile(
    id = id,
    doctorId = doctorId,
    kmpdcRegNumber = kmpdcRegNumber,
    title = title,
    bio = bio,
    yearsOfExperience = yearsOfExperience,
    languages = languages,
    facilityName = facilityName,
    averageRating = averageRating,
    totalReviews = totalReviews,
    totalConsultations = totalConsultations,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PractitionerProfile.toEntity() = PractitionerProfileEntity(
    id = id,
    doctorId = doctorId,
    kmpdcRegNumber = kmpdcRegNumber,
    title = title,
    bio = bio,
    yearsOfExperience = yearsOfExperience,
    languages = languages,
    facilityName = facilityName,
    averageRating = averageRating,
    totalReviews = totalReviews,
    totalConsultations = totalConsultations,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
