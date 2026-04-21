package ke.co.smartroundclinic.common

interface PolicyGroupPermissionResolver {
    suspend fun resolvePermissions(policyGroupId: String): List<String>
}
