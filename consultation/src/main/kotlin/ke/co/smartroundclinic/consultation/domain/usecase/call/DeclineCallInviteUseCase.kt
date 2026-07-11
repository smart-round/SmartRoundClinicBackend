package ke.co.smartroundclinic.consultation.domain.usecase.call

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.consultation.domain.model.CallInviteState
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationCallDeclinedEventRes
import ke.co.smartroundclinic.infra.redis.RedisKeys
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Callee explicitly declined a ringing invite — tells the caller to stop ringing right away. */
class DeclineCallInviteUseCase(
    private val redis: RedisRepository,
    private val socketRegistry: ConsultationSocketRegistry,
    private val notificationSender: NotificationSender? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(callId: String, calleeId: String): DefaultResponse<Unit?> {
        val raw = redis.get(RedisKeys.callInvite(callId))
            ?: return DefaultResponse(httpStatusCode = HttpStatusCode.NotFound.value, status = false, message = "Call invite not found or already resolved", data = null)
        val invite = json.decodeFromString<CallInviteState>(raw)

        if (invite.calleeId != calleeId) {
            return DefaultResponse(httpStatusCode = HttpStatusCode.Forbidden.value, status = false, message = "Not the recipient of this call", data = null)
        }

        redis.delete(RedisKeys.callInvite(callId))
        redis.delete(RedisKeys.activeCallForThread(invite.doctorId, invite.patientId))

        val threadKey = ConsultationSocketRegistry.threadKey(invite.doctorId, invite.patientId)
        runCatching {
            socketRegistry.sendToUser(threadKey, invite.callerId, json.encodeToString(ConsultationCallDeclinedEventRes(callId = callId)))
        }
        runCatching {
            notificationSender?.sendCallSignal(
                event = PushNotificationEvents.CALL_DECLINED,
                recipientId = invite.callerId,
                metadata = mapOf(
                    "callId" to callId,
                    "doctorId" to invite.doctorId,
                    "patientId" to invite.patientId,
                ),
            )
        }

        return DefaultResponse(httpStatusCode = HttpStatusCode.OK.value, status = true, message = "Declined", data = null)
    }
}
