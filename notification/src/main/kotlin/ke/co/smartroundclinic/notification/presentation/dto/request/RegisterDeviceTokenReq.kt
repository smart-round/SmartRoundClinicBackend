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
    // Nullable rather than a non-null default: a client sending an explicit JSON `"tokenType":
    // null` (or an older build that predates this field entirely, depending on serializer
    // config) can still land a null here — coalesce explicitly in toModel() instead of relying
    // on the non-null default alone, which previously NPE'd on UserDeviceToken's constructor.
    val tokenType: TokenType? = null,
) {
    fun toModel(userId: String, userType: String) = UserDeviceToken(
        id = ObjectId().toString(),
        userId = userId,
        userType = runCatching { UserType.valueOf(userType) }.getOrDefault(UserType.PATIENT),
        deviceToken = deviceToken,
        platform = platform,
        tokenType = tokenType ?: TokenType.STANDARD,
        createdAt = Clock.System.now().toString(),
        lastUsedAt = Clock.System.now().toString(),
    )
}
