package ke.co.smartroundclinic.doctor.presentation.dto.request

import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfile
import ke.co.smartroundclinic.doctor.domain.model.PractitionerProfileUpdate
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class CreatePractitionerProfileReq(
    val kmpdcRegNumber: String? = null,
    val title: String? = null,
    val bio: String? = null,
    val yearsOfExperience: Int? = null,
    val languages: List<String> = emptyList(),
    val facilityName: String? = null,
) {
    fun toModel(doctorId: String) = PractitionerProfile(
        id = ObjectId().toString(),
        doctorId = doctorId,
        kmpdcRegNumber = kmpdcRegNumber,
        title = title,
        bio = bio,
        yearsOfExperience = yearsOfExperience,
        languages = languages,
        facilityName = facilityName,
        averageRating = 0.0,
        totalReviews = 0,
        totalConsultations = 0,
        createdAt = System.currentTimeMillis(),
        updatedAt = null,
    )
}

@Serializable
data class UpdatePractitionerProfileReq(
    val kmpdcRegNumber: String? = null,
    val title: String? = null,
    val bio: String? = null,
    val yearsOfExperience: Int? = null,
    val languages: List<String>? = null,
    val facilityName: String? = null,
) {
    fun toDomain() = PractitionerProfileUpdate(
        kmpdcRegNumber = kmpdcRegNumber,
        title = title,
        bio = bio,
        yearsOfExperience = yearsOfExperience,
        languages = languages,
        facilityName = facilityName,
    )
}
