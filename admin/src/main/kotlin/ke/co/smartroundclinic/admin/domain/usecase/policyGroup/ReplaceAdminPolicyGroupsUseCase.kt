package ke.co.smartroundclinic.admin.domain.usecase.policyGroup

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.repository.PolicyGroupRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource

class ReplaceAdminPolicyGroupsUseCase(private val repository: PolicyGroupRepository) {
    suspend operator fun invoke(adminUserId: String, policyGroupIds: List<String>): DefaultResponse<Nothing?> =
        when (val result = repository.replaceAdminPolicyGroups(adminUserId, policyGroupIds)) {
            is Resource.Success -> result.toDefaultResponse(HttpStatusCode.OK.value) { null }
            is Resource.Error -> result.toDefaultResponse(HttpStatusCode.BadRequest.value) { null }
        }
}
