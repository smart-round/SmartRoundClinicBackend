package ke.co.smartroundclinic.doctorchat.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.doctorchat.domain.model.DoctorCallInviteState
import ke.co.smartroundclinic.doctorchat.domain.service.DoctorChatSocketRegistry
import ke.co.smartroundclinic.doctorchat.presentation.dto.response.DoctorCallDeclinedEventRes
import ke.co.smartroundclinic.infra.redis.RedisKeys
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Callee explicitly declined a ringing invite — tells the caller to stop ringing right away. */
class DeclineDoctorCallInviteUseCase(
    private val redis: RedisRepository,
    private val socketRegistry: DoctorChatSocketRegistry,
    private val notificationSender: NotificationSender? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(callId: String, calleeId: String): DefaultResponse<Unit?> {
        val raw = redis.get(RedisKeys.callInvite(callId))
            ?: return DefaultResponse(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Call invite not found or already resolved", data = null)
        val invite = json.decodeFromString<DoctorCallInviteState>(raw)

        if (invite.calleeId != calleeId) {
            return DefaultResponse(httpStatusCode = HttpStatusCode.Forbidden.value, status = false, message = "Not the recipient of this call", data = null)
        }

        redis.delete(RedisKeys.callInvite(callId))
        redis.delete(RedisKeys.activeCallForDoctorChatThread(invite.threadId))

        runCatching {
            socketRegistry.sendToUser(invite.threadId, invite.callerId, json.encodeToString(DoctorCallDeclinedEventRes(callId = callId)))
        }
        runCatching {
            notificationSender?.sendCallSignal(
                event = PushNotificationEvents.CALL_DECLINED,
                recipientId = invite.callerId,
                metadata = mapOf("callId" to callId, "threadId" to invite.threadId),
            )
        }

        return DefaultResponse(httpStatusCode = HttpStatusCode.OK.value, status = true, message = "Declined", data = null)
    }
}
