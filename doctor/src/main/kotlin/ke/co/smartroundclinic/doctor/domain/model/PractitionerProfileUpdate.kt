package ke.co.smartroundclinic.doctor.domain.model

data class PractitionerProfileUpdate(
    val kmpdcRegNumber: String? = null,
    val title: String? = null,
    val bio: String? = null,
    val yearsOfExperience: Int? = null,
    val languages: List<String>? = null,
    val facilityName: String? = null,
)