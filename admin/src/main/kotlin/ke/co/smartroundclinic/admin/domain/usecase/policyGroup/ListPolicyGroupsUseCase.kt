package ke.co.smartroundclinic.admin.domain.usecase.policyGroup

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.repository.PermissionCatalogRepository
import ke.co.smartroundclinic.admin.domain.repository.PolicyGroupRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.PolicyGroupRes
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource

class ListPolicyGroupsUseCase(
    private val repository: PolicyGroupRepository,
    private val catalogRepository: PermissionCatalogRepository,
) {
    suspend operator fun invoke(): DefaultResponse<List<PolicyGroupRes>?> =
        when (val result = repository.getAll()) {
            is Resource.Success -> {
                val groups = result.data ?: emptyList()
                val allKeys = groups.flatMap { it.permissions }.distinct()
                val catalogsByKey = catalogRepository.getByKeys(allKeys).associateBy { it.key }
                result.toDefaultResponse(HttpStatusCode.OK.value) {
                    groups.map { group ->
                        group.toRes(group.permissions.mapNotNull { catalogsByKey[it] })
                    }
                }
            }
            is Resource.Error -> result.toDefaultResponse(HttpStatusCode.InternalServerError.value) { null }
        }
}
