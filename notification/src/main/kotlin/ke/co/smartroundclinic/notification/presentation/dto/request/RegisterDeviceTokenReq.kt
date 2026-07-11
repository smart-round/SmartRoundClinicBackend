package ke.co.smartroundclinic.notification.presentation.dto.request

import ke.co.smartroundclinic.notification.domain.model.Platform
import ke.co.smartroundclinic.notification.domain.model.TokenType
import ke.co.smartroundclinic.notification.domain.model.UserDeviceToken
import ke.co.smartroundclinic.notification.domain.model.UserType
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

@Serializable
data class RegisterDeviceTokenReq(
    val deviceToken: String,
    val platform: Platform,
    val tokenType: TokenType = TokenType.STANDARD,
) {
    fun toModel(userId: String, userType: String) = UserDeviceToken(
        id = ObjectId().toString(),
        userId = userId,
        userType = runCatching { UserType.valueOf(userType) }.getOrDefault(UserType.PATIENT),
        deviceToken = deviceToken,
        platform = platform,
        tokenType = tokenType,
        createdAt = Clock.System.now().toString(),
        lastUsedAt = Clock.System.now().toString(),
    )
}
