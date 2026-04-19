package ke.co.smartroundclinic.support.domain.model

data class IssueCategory(
    val id: String,
    val name: String,
    val description: String? = null,
    val status: Boolean,
    val createdAt: String,
    val updatedAt: String? = null,
)
