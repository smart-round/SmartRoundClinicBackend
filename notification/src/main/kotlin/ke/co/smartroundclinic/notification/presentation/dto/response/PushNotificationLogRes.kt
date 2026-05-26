package ke.co.smartroundclinic.notification.presentation.dto.response

import ke.co.smartroundclinic.notification.domain.model.PushNotificationLog
import kotlinx.serialization.Serializable

@Serializable
data class PushNotificationLogRes(
    val id: String,
    val title: String,
    val body: String,
    val deviceToken: String,
    val userId: String,
    val userType: String,
    val status: String,
    val messageId: String?,
    val error: String?,
    val sentAt: String,
)

@Serializable
data class PushNotificationLogPageResult(
    val items: List<PushNotificationLogRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

@Serializable
data class PushNotificationSummaryRes(
    val sent: Int,
    val failed: Int,
    val total: Int,
)

fun PushNotificationLog.toRes() = PushNotificationLogRes(
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
