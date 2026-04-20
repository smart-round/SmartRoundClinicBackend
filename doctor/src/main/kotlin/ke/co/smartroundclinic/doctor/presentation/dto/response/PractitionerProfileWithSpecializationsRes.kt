package ke.co.smartroundclinic.doctor.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class SpecializationWithNamesRes(
    val id: String,
    val specializationId: String,
    val specializationName: String,
    val subSpecializationId: String?,
    val subSpecializationName: String?,
)

@Serializable
data class PractitionerProfileWithSpecializationsRes(
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
    val specializations: List<SpecializationWithNamesRes>,
    val createdAt: String,
    val updatedAt: String?,
)
