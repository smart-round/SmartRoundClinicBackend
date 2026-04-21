package ke.co.smartroundclinic.admin.domain.model

data class PolicyGroup(
    val id: String,
    val name: String,
    val description: String?,
    val permissions: List<String>,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String? = null,
)
