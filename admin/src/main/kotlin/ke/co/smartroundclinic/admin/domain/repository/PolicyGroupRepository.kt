package ke.co.smartroundclinic.admin.domain.repository

import ke.co.smartroundclinic.admin.domain.model.PolicyGroup
import ke.co.smartroundclinic.common.Resource

interface PolicyGroupRepository {
    suspend fun create(group: PolicyGroup): Resource<PolicyGroup?>
    suspend fun getById(id: String): Resource<PolicyGroup?>
    suspend fun getAll(): Resource<List<PolicyGroup>>
    suspend fun update(id: String, name: String?, description: String?, permissions: List<String>?): Resource<PolicyGroup?>
    suspend fun delete(id: String): Resource<Nothing>
    suspend fun assignAdmin(policyGroupId: String, adminUserId: String): Resource<Nothing>
    suspend fun removeAdmin(policyGroupId: String, adminUserId: String): Resource<Nothing>
    suspend fun resolvePermissions(policyGroupId: String): List<String>
}
