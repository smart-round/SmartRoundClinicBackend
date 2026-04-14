package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfile
import kotlinx.serialization.Serializable

@Serializable
data class PractitionerProfileRes(
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
    val createdAt: Long,
    val updatedAt: Long?,
)

@Serializable
data class PractitionerProfilePageResult(
    val items: List<PractitionerProfileRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun PractitionerProfile.toRes() = PractitionerProfileRes(
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
