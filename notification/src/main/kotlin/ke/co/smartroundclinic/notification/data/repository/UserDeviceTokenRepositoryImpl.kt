package ke.co.smartroundclinic.notification.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
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
import kotlin.time.Duration.Companion.days
import org.bson.Document
import org.slf4j.LoggerFactory

private const val MAX_STANDARD_TOKENS_PER_USER = 5

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
                // A device's FCM token rotates over the life of an install (and each reinstall mints
                // a fresh one), and registration only ever upserts by exact token value — a rotated or
                // reinstalled token is a *new* document, not a replacement of the old one. Left alone
                // this grows without bound; cap it here so every new registration for a user evicts
                // its own least-recently-used STANDARD token once the user is already at the limit.
                if (entity.tokenType == TokenType.STANDARD.name) {
                    evictExcessTokens(entity.userId, TokenType.STANDARD.name, MAX_STANDARD_TOKENS_PER_USER)
                }
                Resource.Success(data = token, message = "Device token registered successfully")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to register device token")
            }
        }

    /** Keeps at most [max] tokens of [tokenType] for [userId], dropping the least-recently-used ones. */
    private suspend fun evictExcessTokens(userId: String, tokenType: String, max: Int): Int {
        val docs = rawCollection.find(
            Filters.and(
                Filters.eq(UserDeviceTokenEntity::userId.name, userId),
                Filters.eq(UserDeviceTokenEntity::tokenType.name, tokenType),
            )
        ).sort(Sorts.descending(UserDeviceTokenEntity::lastUsedAt.name)).toList()

        if (docs.size <= max) return 0

        val idsToRemove = docs.drop(max).mapNotNull { it.getString(UserDeviceTokenEntity::id.name) }
        if (idsToRemove.isEmpty()) return 0

        collection.deleteMany(Filters.`in`(UserDeviceTokenEntity::id.name, idsToRemove))
        logger.info("Evicted ${idsToRemove.size} excess $tokenType token(s) for userId=$userId (cap=$max)")
        return idsToRemove.size
    }

    override suspend fun pruneStaleAndExcessTokens(staleAfterDays: Int, maxPerUser: Int): Resource<Int> =
        withContext(Dispatchers.IO) {
            try {
                var removed = 0

                // Tokens nobody has (re)registered in a long time are almost certainly a dead install
                // (uninstalled app, or a token FCM itself already stopped honoring) — see 80a189f-style
                // dead-token discovery: sendCallSignal/sendDataOnly discover this per-send but never
                // delete, so without a sweep like this the collection only ever grows.
                val staleThreshold = (Clock.System.now() - staleAfterDays.days).toString()
                removed += collection.deleteMany(
                    Filters.lt(UserDeviceTokenEntity::lastUsedAt.name, staleThreshold)
                ).deletedCount.toInt()

                // Catches accounts that were already over MAX_STANDARD_TOKENS_PER_USER before this cap
                // existed — register() only trims on its own next call, which may never come.
                val userIds = rawCollection.distinct(
                    UserDeviceTokenEntity::userId.name,
                    Filters.eq(UserDeviceTokenEntity::tokenType.name, TokenType.STANDARD.name),
                    String::class.java,
                ).toList()
                userIds.forEach { userId ->
                    removed += evictExcessTokens(userId, TokenType.STANDARD.name, maxPerUser)
                }

                Resource.Success(data = removed, message = "Pruned $removed device token(s)")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to prune device tokens")
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
