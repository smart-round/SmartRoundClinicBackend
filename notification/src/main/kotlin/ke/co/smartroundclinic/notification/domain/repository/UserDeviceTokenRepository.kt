package ke.co.smartroundclinic.notification.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.domain.model.UserDeviceToken
import ke.co.smartroundclinic.notification.domain.model.UserType

interface UserDeviceTokenRepository {
    suspend fun register(token: UserDeviceToken): Resource<UserDeviceToken?>
    suspend fun unregister(tokenId: String, userId: String): Resource<UserDeviceToken?>
    suspend fun getByUser(userId: String): Resource<List<UserDeviceToken>>
    suspend fun getByUserType(userType: UserType): Resource<List<UserDeviceToken>>
    suspend fun getAll(): Resource<List<UserDeviceToken>>

    /** Deletes tokens unused for [staleAfterDays] days, then trims every user back down to [maxPerUser]. */
    suspend fun pruneStaleAndExcessTokens(staleAfterDays: Int, maxPerUser: Int): Resource<Int>
}
