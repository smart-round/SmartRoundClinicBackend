package ke.co.smartroundclinic.notification.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.data.entity.UserDeviceTokenEntity
import ke.co.smartroundclinic.notification.data.entity.toEntity
import ke.co.smartroundclinic.notification.domain.model.UserDeviceToken
import ke.co.smartroundclinic.notification.domain.model.UserType
import ke.co.smartroundclinic.notification.domain.repository.UserDeviceTokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class UserDeviceTokenRepositoryImpl(database: MongoDatabase) : UserDeviceTokenRepository {

    private val collection = database.getCollection<UserDeviceTokenEntity>(MongoDBConstants.USER_DEVICE_TOKENS)

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
                val items = collection.find(Filters.eq(UserDeviceTokenEntity::userId.name, userId)).toList()
                Resource.Success(data = items.map { it.toModel() }, message = "Tokens retrieved")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve device tokens")
            }
        }

    override suspend fun getByUserType(userType: UserType): Resource<List<UserDeviceToken>> =
        withContext(Dispatchers.IO) {
            try {
                val items = collection.find(
                    Filters.eq(UserDeviceTokenEntity::userType.name, userType.name)
                ).toList()
                Resource.Success(data = items.map { it.toModel() }, message = "Tokens retrieved")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve device tokens")
            }
        }

    override suspend fun getAll(): Resource<List<UserDeviceToken>> =
        withContext(Dispatchers.IO) {
            try {
                val items = collection.find().toList()
                Resource.Success(data = items.map { it.toModel() }, message = "Tokens retrieved")
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to retrieve device tokens")
            }
        }
}
