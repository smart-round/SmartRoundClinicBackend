package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.infra.redis.RedisKeys

class NotifyOfflineConsultationParticipantUseCase(
    private val redis: RedisRepository,
    private val notificationSender: NotificationSender?,
) {
    suspend operator fun invoke(
        recipientId: String,
        senderName: String,
        messagePreview: String,
        recipientDestination: NotificationDestination,
    ) {
        val presence = redis.get(RedisKeys.presence(recipientId))
        val isOnline = presence == "true"
        if (isOnline) return

        notificationSender?.send(
            title = PushNotificationEvents.newChatMessage(senderName),
            message = messagePreview.take(100),
            channel = NotificationChannel.PUSH_NOTIFICATION,
            destination = recipientDestination,
            recipientId = recipientId,
            metadata = mapOf("event" to PushNotificationEvents.NEW_CHAT_MESSAGE),
        )
    }
}
