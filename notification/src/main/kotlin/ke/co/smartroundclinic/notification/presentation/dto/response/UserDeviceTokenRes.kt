package ke.co.smartroundclinic.notification.presentation.dto.response

import ke.co.smartroundclinic.notification.domain.model.UserDeviceToken
import kotlinx.serialization.Serializable

@Serializable
data class UserDeviceTokenRes(
    val id: String,
    val userId: String,
    val userType: String,
    val deviceToken: String,
    val platform: String,
    val createdAt: String,
    val lastUsedAt: String,
)

fun UserDeviceToken.toRes() = UserDeviceTokenRes(
    id = id,
    userId = userId,
    userType = userType.name,
    deviceToken = deviceToken,
    platform = platform.name,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)
