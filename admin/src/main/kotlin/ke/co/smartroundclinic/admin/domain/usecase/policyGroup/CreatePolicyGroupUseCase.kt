package ke.co.smartroundclinic.admin.domain.usecase.policyGroup

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.model.PolicyGroup
import ke.co.smartroundclinic.admin.domain.repository.PolicyGroupRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource

class CreatePolicyGroupUseCase(private val repository: PolicyGroupRepository) {
    suspend operator fun invoke(group: PolicyGroup): DefaultResponse<PolicyGroup?> =
        when (val result = repository.create(group)) {
            is Resource.Success -> result.toDefaultResponse(HttpStatusCode.Created.value) { it }
            is Resource.Error -> result.toDefaultResponse(HttpStatusCode.BadRequest.value) { null }
        }
}
