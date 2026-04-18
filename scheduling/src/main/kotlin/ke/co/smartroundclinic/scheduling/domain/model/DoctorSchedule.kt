package ke.co.smartroundclinic.scheduling.domain.model

data class DoctorSchedule(
    val id: String,
    val doctorId: String,
    val dayOfWeek: Int,
    val windowStart: String,
    val windowEnd: String,
    val slotDuration: Int,
    val breakBlocks: List<BreakBlock>,
    val isActive: Boolean,
    val timezone: String,
    val createdAt: String,
    val updatedAt: String? = null,
)
