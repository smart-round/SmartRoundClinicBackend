package ke.co.smartroundclinic.scheduling.domain.model

data class SlotOverride(
    val id: String,
    val doctorId: String,
    val date: String,
    val type: String,
    val start: String,
    val end: String,
    val reason: String? = null,
    val createdAt: String,
)
