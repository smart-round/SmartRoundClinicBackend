package ke.co.smartroundclinic.notification.domain.model

enum class Platform { ANDROID, IOS }
enum class UserType { DOCTOR, PATIENT }

data class UserDeviceToken(
    val id: String,
    val userId: String,
    val userType: UserType,
    val deviceToken: String,
    val platform: Platform,
    val createdAt: String,
    val lastUsedAt: String,
)
