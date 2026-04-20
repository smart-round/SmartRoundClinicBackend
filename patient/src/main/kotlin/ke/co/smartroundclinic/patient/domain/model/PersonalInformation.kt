package ke.co.smartroundclinic.patient.domain.model

import ke.co.smartroundclinic.patient.data.entity.Gender
import ke.co.smartroundclinic.patient.data.entity.HeightIn
import ke.co.smartroundclinic.patient.data.entity.MaritalStatus
import ke.co.smartroundclinic.patient.data.entity.WeightIn

data class PersonalInformation(
    val id: String,
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
    val createdAt: String,
    val updatedAt: String? = null,
)
