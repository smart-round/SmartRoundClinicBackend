package ke.co.smartroundclinic.admin.presentation.dto.request

import ke.co.smartroundclinic.admin.domain.model.PolicyGroup
import org.bson.types.ObjectId
import kotlin.time.Clock

data class CreatePolicyGroupReq(
    val name: String,
    val description: String? = null,
    val permissions: List<String> = emptyList(),
) {
    fun toModel(createdBy: String) = PolicyGroup(
        id = ObjectId().toString(),
        name = name,
        description = description,
        permissions = permissions,
        createdBy = createdBy,
        createdAt = Clock.System.now().toString(),
    )
}

data class UpdatePolicyGroupReq(
    val name: String? = null,
    val description: String? = null,
    val permissions: List<String>? = null,
)

data class ReplaceAdminPolicyGroupsReq(
    val adminId: String,
    val policyGroupIds: List<String> = emptyList(),
)
