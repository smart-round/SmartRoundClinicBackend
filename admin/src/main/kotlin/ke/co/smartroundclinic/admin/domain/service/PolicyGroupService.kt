package ke.co.smartroundclinic.admin.domain.service

import ke.co.smartroundclinic.admin.domain.usecase.permissionCatalog.GetPermissionCatalogUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.AssignAdminToPolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.CreatePolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.DeletePolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.GetPolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.ListPolicyGroupsUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.RemoveAdminFromPolicyGroupUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.ReplaceAdminPolicyGroupsUseCase
import ke.co.smartroundclinic.admin.domain.usecase.policyGroup.UpdatePolicyGroupUseCase
import ke.co.smartroundclinic.admin.presentation.dto.request.CreatePolicyGroupReq
import ke.co.smartroundclinic.admin.presentation.dto.request.UpdatePolicyGroupReq

class PolicyGroupService(
    private val createUseCase: CreatePolicyGroupUseCase,
    private val getUseCase: GetPolicyGroupUseCase,
    private val listUseCase: ListPolicyGroupsUseCase,
    private val updateUseCase: UpdatePolicyGroupUseCase,
    private val deleteUseCase: DeletePolicyGroupUseCase,
    private val assignUseCase: AssignAdminToPolicyGroupUseCase,
    private val removeUseCase: RemoveAdminFromPolicyGroupUseCase,
    private val replaceUseCase: ReplaceAdminPolicyGroupsUseCase,
    private val getPermissionCatalogUseCase: GetPermissionCatalogUseCase,
) {
    suspend fun create(req: CreatePolicyGroupReq, createdBy: String) = createUseCase(req.toModel(createdBy))
    suspend fun get(id: String) = getUseCase(id)
    suspend fun list() = listUseCase()
    suspend fun update(id: String, req: UpdatePolicyGroupReq) = updateUseCase(id, req.name, req.description, req.permissions)
    suspend fun delete(id: String) = deleteUseCase(id)
    suspend fun assignAdmin(policyGroupId: String, adminUserId: String) = assignUseCase(policyGroupId, adminUserId)
    suspend fun removeAdmin(policyGroupId: String, adminUserId: String) = removeUseCase(policyGroupId, adminUserId)
    suspend fun replaceAdminPolicyGroups(adminUserId: String, policyGroupIds: List<String>) = replaceUseCase(adminUserId, policyGroupIds)
    suspend fun getPermissionCatalog() = getPermissionCatalogUseCase()
}
