package ke.co.smartroundclinic.admin.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.admin.data.entity.PolicyGroupEntity
import ke.co.smartroundclinic.admin.data.entity.toEntity
import ke.co.smartroundclinic.admin.domain.model.PolicyGroup
import ke.co.smartroundclinic.admin.domain.repository.PolicyGroupRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.PolicyGroupPermissionResolver
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import kotlin.time.Clock

class PolicyGroupRepositoryImpl(
    adminDatabase: MongoDatabase,
    authDatabase: MongoDatabase,
) : PolicyGroupRepository, PolicyGroupPermissionResolver {

    private val groups = adminDatabase.getCollection<PolicyGroupEntity>(MongoDBConstants.ADMIN_POLICY_GROUPS)
    private val users = authDatabase.getCollection<Document>(MongoDBConstants.AUTH_USER)

    override suspend fun create(group: PolicyGroup): Resource<PolicyGroup?> = withContext(Dispatchers.IO) {
        try {
            val entity = group.toEntity()
            groups.insertOne(entity)
            Resource.Success(data = entity.toModel(), message = "Policy group created successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create policy group")
        }
    }

    override suspend fun getById(id: String): Resource<PolicyGroup?> = withContext(Dispatchers.IO) {
        val entity = groups.find(Filters.eq(PolicyGroupEntity::id.name, id)).firstOrNull()
            ?: return@withContext Resource.Error("Policy group not found")
        Resource.Success(data = entity.toModel(), message = "Policy group found")
    }

    override suspend fun getAll(): Resource<List<PolicyGroup>> = withContext(Dispatchers.IO) {
        try {
            val result = groups.find().toList().map { it.toModel() }
            Resource.Success(data = result, message = "Policy groups fetched successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch policy groups")
        }
    }

    override suspend fun update(
        id: String,
        name: String?,
        description: String?,
        permissions: List<String>?,
    ): Resource<PolicyGroup?> = withContext(Dispatchers.IO) {
        try {
            groups.find(Filters.eq(PolicyGroupEntity::id.name, id)).firstOrNull()
                ?: return@withContext Resource.Error("Policy group not found")
            val updates = mutableListOf(Updates.set(PolicyGroupEntity::updatedAt.name, Clock.System.now().toString()))
            name?.let { updates.add(Updates.set(PolicyGroupEntity::name.name, it)) }
            description?.let { updates.add(Updates.set(PolicyGroupEntity::description.name, it)) }
            permissions?.let { updates.add(Updates.set(PolicyGroupEntity::permissions.name, it)) }
            groups.updateOne(Filters.eq(PolicyGroupEntity::id.name, id), Updates.combine(updates))
            val updated = groups.find(Filters.eq(PolicyGroupEntity::id.name, id)).firstOrNull()
            Resource.Success(data = updated?.toModel(), message = "Policy group updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update policy group")
        }
    }

    override suspend fun delete(id: String): Resource<Nothing> = withContext(Dispatchers.IO) {
        try {
            val result = groups.deleteOne(Filters.eq(PolicyGroupEntity::id.name, id))
            if (result.deletedCount == 0L) return@withContext Resource.Error("Policy group not found")
            Resource.Success(data = null, message = "Policy group deleted successfully")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete policy group")
        }
    }

    override suspend fun assignAdmin(policyGroupId: String, adminUserId: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                groups.find(Filters.eq(PolicyGroupEntity::id.name, policyGroupId)).firstOrNull()
                    ?: return@withContext Resource.Error("Policy group not found")
                val result = users.updateOne(
                    Filters.eq("id", adminUserId),
                    Updates.set("policyGroupId", policyGroupId)
                )
                if (result.matchedCount == 0L) return@withContext Resource.Error("Admin user not found")
                Resource.Success(data = null, message = "Admin assigned to policy group successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to assign admin to policy group")
            }
        }

    override suspend fun removeAdmin(policyGroupId: String, adminUserId: String): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                val result = users.updateOne(
                    Filters.and(
                        Filters.eq("id", adminUserId),
                        Filters.eq("policyGroupId", policyGroupId)
                    ),
                    Updates.set("policyGroupId", null)
                )
                if (result.matchedCount == 0L) return@withContext Resource.Error("Admin not found in this policy group")
                Resource.Success(data = null, message = "Admin removed from policy group successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to remove admin from policy group")
            }
        }

    override suspend fun resolvePermissions(policyGroupId: String): List<String> = withContext(Dispatchers.IO) {
        groups.find(Filters.eq(PolicyGroupEntity::id.name, policyGroupId)).firstOrNull()?.permissions ?: emptyList()
    }
}
