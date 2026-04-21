package ke.co.smartroundclinic.common

interface PolicyGroupPermissionResolver {
    suspend fun resolvePermissions(policyGroupIds: List<String>): List<String>
}
