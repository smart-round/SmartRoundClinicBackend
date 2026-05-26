package ke.co.smartroundclinic.notification.data.entity

import ke.co.smartroundclinic.notification.domain.model.PushNotificationLog
import ke.co.smartroundclinic.notification.domain.model.PushNotificationLogStatus
import ke.co.smartroundclinic.notification.domain.model.UserType
import org.bson.types.ObjectId
import kotlin.time.Clock

data class PushNotificationLogEntity(
    val id: String = ObjectId().toString(),
    val title: String,
    val body: String,
    val deviceToken: String,
    val userId: String,
    val userType: String? = null,
    val status: String,
    val messageId: String? = null,
    val error: String? = null,
    val sentAt: String = Clock.System.now().toString(),
) {
    fun toModel() = PushNotificationLog(
        id = id,
        title = title,
        body = body,
        deviceToken = deviceToken,
        userId = userId,
        userType = runCatching { UserType.valueOf(userType ?: "") }.getOrDefault(UserType.PATIENT),
        status = runCatching { PushNotificationLogStatus.valueOf(status) }.getOrDefault(PushNotificationLogStatus.FAILED),
        messageId = messageId,
        error = error,
        sentAt = sentAt,
    )
}

fun PushNotificationLog.toEntity() = PushNotificationLogEntity(
    id = id,
    title = title,
    body = body,
    deviceToken = deviceToken,
    userId = userId,
    userType = userType.name,
    status = status.name,
    messageId = messageId,
    error = error,
    sentAt = sentAt,
)
