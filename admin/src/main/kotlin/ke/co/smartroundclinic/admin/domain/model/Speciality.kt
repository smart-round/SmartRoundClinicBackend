package ke.co.smartroundclinic.admin.domain.model

data class Speciality(
    val id: String,
    val serviceTierId: String? = null,
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
)

data class Subspecialty(
    val id: String,
    val specialityId: String,
    val title: String,
    val description: String,
    val color: String = "#FFFFFF",
    val iconUrl: String? = null,
)
