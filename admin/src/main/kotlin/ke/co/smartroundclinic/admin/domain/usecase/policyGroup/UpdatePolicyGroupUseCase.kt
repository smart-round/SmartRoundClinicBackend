package ke.co.smartroundclinic.admin.domain.usecase.policyGroup

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.model.PolicyGroup
import ke.co.smartroundclinic.admin.domain.repository.PolicyGroupRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource

class UpdatePolicyGroupUseCase(private val repository: PolicyGroupRepository) {
    suspend operator fun invoke(
        id: String,
        name: String?,
        description: String?,
        permissions: List<String>?,
    ): DefaultResponse<PolicyGroup?> =
        when (val result = repository.update(id, name, description, permissions)) {
            is Resource.Success -> result.toDefaultResponse(HttpStatusCode.OK.value) { it }
            is Resource.Error -> result.toDefaultResponse(HttpStatusCode.BadRequest.value) { null }
        }
}
