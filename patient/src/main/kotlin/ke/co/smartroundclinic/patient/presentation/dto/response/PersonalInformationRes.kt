package ke.co.smartroundclinic.patient.presentation.dto.response

import ke.co.smartroundclinic.patient.data.entity.Gender
import ke.co.smartroundclinic.patient.data.entity.HeightIn
import ke.co.smartroundclinic.patient.data.entity.MaritalStatus
import ke.co.smartroundclinic.patient.data.entity.WeightIn
import ke.co.smartroundclinic.patient.domain.model.PersonalInformation
import kotlinx.serialization.Serializable

@Serializable
data class PersonalInformationRes(
    val id: String,
    val patientId: String,
    val gender: Gender,
    val phoneNumber: String,
    val countryCode: String,
    val bloodGroup: String,
    val dateOfBirth: String,
    val weight: Double?,
    val weightIn: WeightIn?,
    val height: Double?,
    val heightIn: HeightIn?,
    val maritalStatus: MaritalStatus?,
    val createdAt: String,
    val updatedAt: String?,
)

fun PersonalInformation.toRes() = PersonalInformationRes(
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
    createdAt = createdAt,
    updatedAt = updatedAt,
)
