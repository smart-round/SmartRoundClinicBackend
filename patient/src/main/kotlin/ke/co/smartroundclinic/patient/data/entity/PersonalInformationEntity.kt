package ke.co.smartroundclinic.patient.data.entity

import ke.co.smartroundclinic.patient.domain.model.PersonalInformation
import org.bson.types.ObjectId
import kotlin.time.Clock

data class PersonalInformationEntity(
    val id: String = ObjectId().toString(),
    val patientId: String,
    val gender: Gender,
    val phoneNumber: String,
    val countryCode: String,
    val bloodGroup: String,
    val dateOfBirth: String,
    val weight: Double? = null,
    val weightIn: WeightIn? = null,
    val height: Double? = null,
    val heightIn: HeightIn? = null,
    val maritalStatus: MaritalStatus? = null,
    val allergies: List<String>? = null,
    val chronicConditions: List<String>? = null,
    val currentMedications: List<String>? = null,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    fun toModel() = PersonalInformation(
        id = id,
        patientId = patientId,
        gender = gender,
        phoneNumber = phoneNumber,
        countryCode = countryCode,
        bloodGroup = bloodGroup,
        dateOfBirth = dateOfBirth,
        weight = weight,
        weightIn = weightIn,
        height = height,
        heightIn = heightIn,
        maritalStatus = maritalStatus,
        allergies = allergies ?: emptyList(),
        chronicConditions = chronicConditions ?: emptyList(),
        currentMedications = currentMedications ?: emptyList(),
        averageRating = averageRating,
        totalReviews = totalReviews,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

enum class Gender { MALE, FEMALE, NON_BINARY, OTHER }
enum class WeightIn { KG, LBS }
enum class HeightIn { CM, INCHES }
enum class MaritalStatus { SINGLE, MARRIED, DIVORCED, WIDOWED }

fun PersonalInformation.toEntity() = PersonalInformationEntity(
    id = id,
    patientId = patientId,
    gender = gender,
    phoneNumber = phoneNumber,
    countryCode = countryCode,
    bloodGroup = bloodGroup,
    dateOfBirth = dateOfBirth,
    weight = weight,
    weightIn = weightIn,
    height = height,
    heightIn = heightIn,
    maritalStatus = maritalStatus,
    allergies = allergies,
    chronicConditions = chronicConditions,
    currentMedications = currentMedications,
    averageRating = averageRating,
    totalReviews = totalReviews,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
