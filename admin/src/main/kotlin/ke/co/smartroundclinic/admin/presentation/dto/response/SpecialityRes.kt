package ke.co.smartroundclinic.admin.presentation.dto.response

data class SpecialityRes(
    val id: String,
    val title: String,
    val description: String,
    val color: String,
    val iconUrl: String?,
)

data class SubSpecialityRes(
    val id: String,
    val specialityId: String,
    val title: String,
    val description: String,
    val color: String,
    val iconUrl: String?,
)
