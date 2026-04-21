package ke.co.smartroundclinic.admin.domain.usecase.policyGroup

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.repository.PermissionCatalogRepository
import ke.co.smartroundclinic.admin.domain.repository.PolicyGroupRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.PolicyGroupRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource

class GetPolicyGroupUseCase(
    private val repository: PolicyGroupRepository,
    private val catalogRepository: PermissionCatalogRepository,
) {
    suspend operator fun invoke(id: String): DefaultResponse<PolicyGroupRes?> =
        when (val result = repository.getById(id)) {
            is Resource.Success -> {
                val group = result.data
                val catalogs = if (group != null) catalogRepository.getByKeys(group.permissions) else emptyList()
                result.toDefaultResponse(HttpStatusCode.OK.value) { it?.toRes(catalogs) }
            }
            is Resource.Error -> result.toDefaultResponse(HttpStatusCode.NotFound.value) { null }
        }
}
