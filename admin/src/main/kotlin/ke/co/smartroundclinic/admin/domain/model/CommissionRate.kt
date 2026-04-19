package ke.co.smartroundclinic.admin.domain.model

data class CommissionRate(
    val id: String,
    val adminId: String,
    val commissionRate: Double,
    val createdAt: String,
    val updatedAt: String? = null,
)
