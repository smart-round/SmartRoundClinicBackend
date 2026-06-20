package ke.co.smartroundclinic.common

data class PatientProfile(
    val id: String,
    val patientId: String,
    val gender: String,
    val phoneNumber: String,
    val countryCode: String,
    val bloodGroup: String,
    val dateOfBirth: String,
    val weight: Double?,
    val weightIn: String?,
    val height: Double?,
    val heightIn: String?,
    val maritalStatus: String?,
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList(),
    val currentMedications: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String?,
)
