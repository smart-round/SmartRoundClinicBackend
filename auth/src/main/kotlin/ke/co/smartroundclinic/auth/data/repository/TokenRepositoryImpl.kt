package ke.co.smartroundclinic.auth.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.auth.data.entity.RevokedTokenEntity
import ke.co.smartroundclinic.auth.domain.repository.TokenRepository
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.time.Clock

class TokenRepositoryImpl(database: MongoDatabase) : TokenRepository {

    private val collection = database.getCollection<RevokedTokenEntity>(MongoDBConstants.AUTH_REVOKED_TOKENS)

    override suspend fun revokeToken(token: String, expiresAt: Long): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            try {
                collection.insertOne(RevokedTokenEntity(token = token, expiresAt = expiresAt))
                Resource.Success(data = null, message = "Token revoked")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to revoke token")
            }
        }

    override suspend fun isRevoked(token: String): Boolean = withContext(Dispatchers.IO) {
        val entry = collection.find(Filters.eq(RevokedTokenEntity::token.name, token)).firstOrNull()
            ?: return@withContext false
        // treat as not-revoked if the stored entry itself has expired (token already invalid anyway)
        entry.expiresAt > Clock.System.now().toEpochMilliseconds()
    }
}