package ke.co.smartroundclinic.notification.data.entity

import ke.co.smartroundclinic.notification.domain.model.Platform
import ke.co.smartroundclinic.notification.domain.model.UserDeviceToken
import ke.co.smartroundclinic.notification.domain.model.UserType
import org.bson.types.ObjectId
import kotlin.time.Clock

data class UserDeviceTokenEntity(
    val id: String = ObjectId().toString(),
    val userId: String,
    val userType: String,
    val deviceToken: String,
    val platform: String,
    val createdAt: String = Clock.System.now().toString(),
    val lastUsedAt: String = Clock.System.now().toString(),
) {
    fun toModel() = UserDeviceToken(
        id = id,
        userId = userId,
        userType = runCatching { UserType.valueOf(userType) }.getOrDefault(UserType.PATIENT),
        deviceToken = deviceToken,
        platform = runCatching { Platform.valueOf(platform) }.getOrDefault(Platform.ANDROID),
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
    )
}

fun UserDeviceToken.toEntity() = UserDeviceTokenEntity(
    id = id,
    userId = userId,
    userType = userType.name,
    deviceToken = deviceToken,
    platform = platform.name,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)
