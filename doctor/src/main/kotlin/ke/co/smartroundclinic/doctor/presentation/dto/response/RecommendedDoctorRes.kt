package ke.co.smartroundclinic.doctor.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class RecommendedDoctorRes(
    val profileId: String,
    val doctorId: String,
    val kmpdcRegNumber: String?,
    val title: String?,
    val bio: String?,
    val yearsOfExperience: Int?,
    val languages: List<String>,
    val facilityName: String?,
    val averageRating: Double,
    val totalReviews: Int,
    val totalBookings: Int,
    val score: Double,
    val specializations: List<SpecializationWithNamesRes>,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class RecommendedDoctorsPageRes(
    val items: List<RecommendedDoctorRes>,
    val total: Long,
    val page: Int,
    val size: Int,
)
