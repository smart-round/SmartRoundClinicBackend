package ke.co.smartroundclinic.notification.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.data.entity.UserDeviceTokenEntity
import ke.co.smartroundclinic.notification.data.entity.toEntity
import ke.co.smartroundclinic.notification.domain.model.TokenType
import ke.co.smartroundclinic.notification.domain.model.UserDeviceToken
import ke.co.smartroundclinic.notification.domain.model.UserType
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import org.bson.Document
import org.slf4j.LoggerFactory

class UserDeviceTokenRepositoryImpl(database: MongoDatabase) : UserDeviceTokenRepository {

    private val collection = database.getCollection<UserDeviceTokenEntity>(MongoDBConstants.USER_DEVICE_TOKENS)
    // Raw-document view used only for reads — the typed Flow<UserDeviceTokenEntity> above decodes
    // eagerly, so a single legacy document predating a newly-added non-null field (e.g. tokenType,
    // added after this collection already had rows) throws and kills results for every OTHER row
    // in the same query too. Decoding through Document with manual per-row fallback isolates that.
    private val rawCollection = database.getCollection<Document>(MongoDBConstants.USER_DEVICE_TOKENS)
    private val logger = LoggerFactory.getLogger(UserDeviceTokenRepositoryImpl::class.java)

    private fun Document.toEntityOrNull(): UserDeviceTokenEntity? = runCatching {
        UserDeviceTokenEntity(
            id = getString("id") ?: get("_id").toString(),
            userId = getString("userId") ?: return@runCatching null,
            userType = getString("userType") ?: return@runCatching null,
            deviceToken = getString("deviceToken") ?: return@runCatching null,
            platform = getString("platform") ?: return@runCatching null,
            tokenType = getString("tokenType") ?: TokenType.STANDARD.name,
            createdAt = getString("createdAt") ?: Clock.System.now().toString(),
            lastUsedAt = getString("lastUsedAt") ?: Clock.System.now().toString(),
        )
    }.onFailure { logger.warn("Skipping malformed user_device_tokens document id=${get("id")}: ${it.message}") }
        .getOrNull()

    override suspend fun register(token: UserDeviceToken): Resource<UserDeviceToken?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = token.toEntity()
                // Upsert on (userId, deviceToken) to avoid duplicate registrations
                collection.replaceOne(
                    Filters.and(
                        Filters.eq(UserDeviceTokenEntity::userId.name, entity.userId),
                        Filters.eq(UserDeviceTokenEntity::deviceToken.name, entity.deviceToken),
                    ),
                    entity,
                    ReplaceOptions().upsert(true),
                )
                Resource.Success(data = token, message = "Device token registered successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to register device token")
            }
        }

    override suspend fun unregister(tokenId: String, userId: String): Resource<UserDeviceToken?> =
        withContext(Dispatchers.IO) {
            try {
                val existing = collection.find(
                    Filters.and(
                        Filters.eq(UserDeviceTokenEntity::id.name, tokenId),
                        Filters.eq(UserDeviceTokenEntity::userId.name, userId),
                    )
                ).firstOrNull() ?: return@withContext Resource.Error("Device token not found")
                collection.deleteOne(Filters.eq(UserDeviceTokenEntity::id.name, tokenId))
                Resource.Success(data = existing.toModel(), message = "Device token removed successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to unregister device token")
            }
        }

    override suspend fun getByUser(userId: String): Resource<List<UserDeviceToken>> =
        withContext(Dispatchers.IO) {
            try {
                val items = rawCollection.find(Filters.eq(UserDeviceTokenEntity::userId.name, userId))
                    .toList().mapNotNull { it.toEntityOrNull() }
                Resource.Success(data = items.map { it.toModel() }, message = "Tokens retrieved")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve device tokens")
            }
        }

    override suspend fun getByUserType(userType: UserType): Resource<List<UserDeviceToken>> =
        withContext(Dispatchers.IO) {
            try {
                val items = rawCollection.find(
                    Filters.eq(UserDeviceTokenEntity::userType.name, userType.name)
                ).toList().mapNotNull { it.toEntityOrNull() }
                Resource.Success(data = items.map { it.toModel() }, message = "Tokens retrieved")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve device tokens")
            }
        }

    override suspend fun getAll(): Resource<List<UserDeviceToken>> =
        withContext(Dispatchers.IO) {
            try {
                val items = rawCollection.find().toList().mapNotNull { it.toEntityOrNull() }
                Resource.Success(data = items.map { it.toModel() }, message = "Tokens retrieved")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve device tokens")
            }
        }
}
