package ke.co.smartroundclinic.patient.presentation.dto.request

import ke.co.smartroundclinic.patient.data.entity.Gender
import ke.co.smartroundclinic.patient.data.entity.HeightIn
import ke.co.smartroundclinic.patient.data.entity.MaritalStatus
import ke.co.smartroundclinic.patient.data.entity.WeightIn
import ke.co.smartroundclinic.patient.domain.model.PersonalInformation
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class CreatePersonalInformationReq(
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
) {
    fun toModel(patientId: String) = PersonalInformation(
        id = ObjectId().toString(),
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
        createdAt = Clock.System.now().toString(),
        updatedAt = null,
    )
}

@Serializable
data class UpdatePersonalInformationReq(
    val phoneNumber: String? = null,
    val countryCode: String? = null,
    val bloodGroup: String? = null,
    val dateOfBirth: String? = null,
    val weight: Double? = null,
    val weightIn: WeightIn? = null,
    val height: Double? = null,
    val heightIn: HeightIn? = null,
    val maritalStatus: MaritalStatus? = null,
    val allergies: List<String>? = null,
    val chronicConditions: List<String>? = null,
    val currentMedications: List<String>? = null,
)
