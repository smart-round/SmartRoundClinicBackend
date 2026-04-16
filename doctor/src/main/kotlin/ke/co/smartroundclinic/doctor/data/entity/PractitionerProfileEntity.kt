package ke.co.smartroundclinic.doctor.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class PractitionerProfileEntity(
    val id: String = ObjectId().toString(),
    val doctorId: String,
    val kmpdcRegNumber: String? = null,
    val title: String? = null,
    val bio: String? = null,
    val yearsOfExperience: Int? = null,
    val languages: List<String> = emptyList(),
    val facilityName: String? = null,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val totalConsultations: Int = 0,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)
