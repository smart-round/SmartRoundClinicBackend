package ke.co.smartroundclinic.notification.domain.model

enum class Platform { ANDROID, IOS }
enum class UserType { DOCTOR, PATIENT }

/** STANDARD = regular FCM/APNs remote-notification token. VOIP = iOS PushKit token (see ApnsVoipClient), used only to ring incoming calls. */
enum class TokenType { STANDARD, VOIP }

data class UserDeviceToken(
    val id: String,
    val userId: String,
    val userType: UserType,
    val deviceToken: String,
    val platform: Platform,
    val tokenType: TokenType = TokenType.STANDARD,
    val createdAt: String,
    val lastUsedAt: String,
)
